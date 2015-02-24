package com.gigmed.riskengine.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import javassist.bytecode.stackmap.TypeData.ClassName;

import javax.xml.crypto.Data;

import org.apache.catalina.tribes.transport.DataSender;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.Pair;
import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Restrictions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVReadProc;

import com.gigmed.riskengine.algorithm.RiskEngineAlgorithms;
import com.gigmed.riskengine.common.C;
import com.gigmed.riskengine.common.HibernateUtil;
import com.gigmed.riskengine.common.Util;
import com.gigmed.riskengine.dao.Anomalyresult;
import com.gigmed.riskengine.dao.Diagnosticvsdrugfrequency;
import com.gigmed.riskengine.dao.Inputdataset;

import fr.prados.xpath4sax.SAXXPath;
import fr.prados.xpath4sax.TextWrapper;
import fr.prados.xpath4sax.XPathSyntaxException;
import fr.prados.xpath4sax.XPathXMLHandler;

//@Service
public class RiskEngineRepository {	
	private static final Logger log = Logger.getLogger( ClassName.class.getName() );
	
	private static RiskEngineRepository instance = null;

	private static double m_riskPercentageThreshold = 0.01;	
	private Map<String, MutableLong> m_diagnosticVSDrugFreq = new TreeMap<String, MutableLong>();
	private Map<String, MutableLong> m_diagnosticVSIngredientsFreq = new TreeMap<String, MutableLong>();
	private long m_totalPrescriptionsCount = 0;
	private Map<String, MutableLong> m_fieldFreq = new TreeMap<String, MutableLong>();
	private Map<String, MutableLong> m_diagnosisFreqWithOtherDiagnoses = new TreeMap<String, MutableLong>();
	protected String m_rxcui;
	protected ArrayList<String> m_ingredients;
	private Set<String> m_invalidDrugNamesArray = new HashSet<String>();
	private int m_anomalySize;

	private Map<String, String[]> m_RXCUIToingredientsCache = new HashMap<String, String[]>();
	private Map<String, String> m_drugNameToRXCUICache = new HashMap<String, String>();
	private HashMap<String,String> m_icd9DescriptionCache = new HashMap<String, String>();
	private HashMap<String,String> m_drugDescriptionCache = new HashMap<String, String>();
	private Logger logger;

	public static final String TEST_BY_DRUG_NAME = "drug";
	public static final String TEST_BY_INGREDIENT = "ingredients";	

	public RiskEngineRepository() {			
	}

	//	public static RiskEngineRepository getInstance(double riskPercentageThreshold) {
	//		if(instance == null) {
	//			instance = new RiskEngineRepository(riskPercentageThreshold);
	//		}
	//		return instance;
	//	}

	public RiskEngineRepository(double riskPercentageThreshold, String workingFolderPath, boolean loadCache) {		
		super();
		logger = Util.getInitializedLogger();

		m_riskPercentageThreshold = riskPercentageThreshold;

		if (loadCache)
			loadCache(workingFolderPath);
	}

	public RiskEngineRepository(double riskPercentageThreshold) {
		this(riskPercentageThreshold, C.CACHE_FOLDER_FILE_PATH, true);
	}

	public void loadCache(String cacheFolderPath) {
		try {
			m_RXCUIToingredientsCache = (Map<String, String[]>) Util.loadCacheFromFile(cacheFolderPath+"RXCUIToingredients.cache");
			m_drugNameToRXCUICache = (Map<String, String>) Util.loadCacheFromFile(cacheFolderPath+"drugNameToRXCUI.cache");
			m_icd9DescriptionCache = (HashMap<String,String>) Util.loadCacheFromFile(cacheFolderPath+"icd9Description.cache");
			m_drugDescriptionCache = (HashMap<String,String>) Util.loadCacheFromFile(cacheFolderPath+"drugDescription.cache");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	

	public void saveCache(String cacheFolderPath) {
		File cacheFolder = new File(cacheFolderPath);
		if (!cacheFolder.isDirectory()) 
			cacheFolder.mkdir();

		Util.saveCacheToFile(cacheFolderPath+"RXCUIToingredients.cache",m_RXCUIToingredientsCache);
		Util.saveCacheToFile(cacheFolderPath+"drugNameToRXCUI.cache",m_drugNameToRXCUICache);
		Util.saveCacheToFile(cacheFolderPath+"icd9Description.cache",m_icd9DescriptionCache);
		Util.saveCacheToFile(cacheFolderPath+"drugDescription.cache",m_drugDescriptionCache);
	}	

	public static double getRiskPercentageThreshold() {
		return m_riskPercentageThreshold;
	}

	public void setRiskPercentageThreshold(double riskPercentageThreshold) {
		m_riskPercentageThreshold = riskPercentageThreshold;
	}

	public String insertRecord(String datasetID, String[] diagnosisCodes, String commercialDrugName, boolean returnIsAnomaly){				
		for (String diagnosisCode:diagnosisCodes){			
			insertRecord(datasetID, diagnosisCode, commercialDrugName, returnIsAnomaly, C.DIAGNOSIS_CODE_TYPE_ICD9_GENERALIZED);				
		}

		if (returnIsAnomaly)
			return checkAnomaly(datasetID, diagnosisCodes, commercialDrugName);
		else
			return null;
	}

	public Anomalyresult insertRecord(String datasetID, String diagnosisCode, String commercialDrugName,
			boolean returnIsAnomaly, int diagnosisCodeTypeIcd9Generalized) {
		return insertRecord(datasetID, diagnosisCode, commercialDrugName, returnIsAnomaly,  diagnosisCodeTypeIcd9Generalized, true);		
	}

	private String checkAnomaly(String datasetID, String[] diagnosisCodes, String commercialDrugName) {
		int diagnosisCounter = 0;
		int anomalyCounter = 0;
		for (String diagnosisCode : diagnosisCodes){
			if (!StringUtil.isEmpty(diagnosisCode))
				diagnosisCounter++;
			if (checkAnomaly(datasetID, diagnosisCode, commercialDrugName)!=null)
				anomalyCounter++;				
		}
		if (anomalyCounter == diagnosisCounter)
			return "Drug "+commercialDrugName+" is rarely given to any of these diagnoses "+diagnosisCodes;
		else
			return null;
	}

	public Anomalyresult insertRecord(String datasetID, String diagnosisCode, String commercialDrugName, boolean returnIsAnomaly, boolean convertDrugToIngredients){
		//		if (!isDrugNameValid(commercialDrugName))
		//			return false;

		if (!convertDrugToIngredients)
			return insertRecord(datasetID, diagnosisCode,commercialDrugName, returnIsAnomaly, C.DIAGNOSIS_CODE_TYPE_ICD9_GENERALIZED);


		Anomalyresult anomalyFound = null;
		try {
			anomalyFound = insertRecord(datasetID, diagnosisCode, getDrugIngredientsByCommercialDrugName(commercialDrugName), returnIsAnomaly);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return anomalyFound;

	}

	public Anomalyresult insertRecord(String datasetID, String diagnosisCode, String commercialDrugName, boolean returnIsAnomaly, boolean convertDrugToIngredients, int diagnosisCodeType){
		if (diagnosisCodeType == C.DIAGNOSIS_CODE_TYPE_ICD9_GENERALIZED)
			diagnosisCode = getGeneralizedICD9Code(diagnosisCode);		

		return insertRecord(datasetID, diagnosisCode, commercialDrugName, returnIsAnomaly, convertDrugToIngredients);

	}

	private String getGeneralizedICD9Code(String diagnosisCode) {
		int dotLocation = diagnosisCode.indexOf('.');
		if (dotLocation>0)			
			return diagnosisCode.substring(0,dotLocation);
		else 
			return diagnosisCode;
	}

	public Anomalyresult insertRecord(String dataSetID, String diagnosisCode,
			String[] drugIngredientsByCommercialDrugName,
			boolean returnIsAnomaly) {	
		IncrementFrequencyIfCombinationExists(diagnosisCode, drugIngredientsByCommercialDrugName);			
		m_totalPrescriptionsCount++;

		return checkAnomalyIfNeeded(dataSetID, diagnosisCode,drugIngredientsByCommercialDrugName, returnIsAnomaly);	
	}

	public String[] getDrugIngredientsByCommercialDrugName(String commercialDrugName) {
		String rxcui = getRXCUIByDrugName(commercialDrugName,true);
		if (StringUtil.isEmpty(rxcui)){			
			//log.info("Drug name \""+commercialDrugName+"\" is Invalid" );
			m_invalidDrugNamesArray.add(commercialDrugName);
		}
		else			
			return getIngredientNamesForRXCUI(rxcui);

		return new String[]{"N/A"};

	}

	private String getApproximateTermRXCUIByDrugName(String commercialDrugName) {
		// TODO Auto-generated method stub
		return null;
	}

	private String[] getIngredientNamesForRXCUI(String rxcui) {
		m_ingredients = new ArrayList<String>();

		// get ingredients from cache if this rxcui was already processed once
		String[] ingredientsArray = m_RXCUIToingredientsCache.get(rxcui);
		if (ingredientsArray!=null)
			return ingredientsArray;		

		XMLReader myReader = null;
		try {
			myReader = XMLReaderFactory.createXMLReader();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		XPathXMLHandler handler=new XPathXMLHandler()
		{
			@Override
			public void findXpathNode(SAXXPath xpath, Object node) {
				m_ingredients.add(((TextWrapper)node).getNodeValue());
			}

		};
		try {
			handler.setXPaths(XPathXMLHandler.toXPaths("/rxnormdata/relatedGroup/conceptGroup/conceptProperties/name/text()"));
		} catch (XPathSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		myReader.setContentHandler(handler);
		try {
			myReader.parse(new InputSource(new URL(C.URL_RXNORM_CONVERT_RXCUI_TO_INGREDIENTS_PART_1+rxcui+C.URL_RXNORM_CONVERT_RXCUI_TO_INGREDIENTS_PART_2).openStream()));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ingredientsArray = new String[m_ingredients.size()];
		m_RXCUIToingredientsCache.put(rxcui,ingredientsArray);
		return m_ingredients.toArray(ingredientsArray);
	}

	private String getRXCUIByDrugName(String commercialDrugName, boolean findAlsoApproximateTerms) {	
		//		String out = new Scanner(new URL("http://rxnav.nlm.nih.gov/REST/rxcui?name=celebrex").openStream(), "UTF-8").useDelimiter("\\A").next();	//test
		//		log.info(out);//test

		// check in cache 
		m_rxcui = m_drugNameToRXCUICache.get(commercialDrugName);
		if (m_rxcui != null)
			return m_rxcui;

		extractRXCUI(commercialDrugName,C.XPATH_RXNORM_ID_TEXT,C.URL_RXNORM_CONVERT_DRUG_NAME_TO_RXCUI);

		if (m_rxcui == null && findAlsoApproximateTerms)
			extractRXCUI(commercialDrugName,C.XPATH_APPROXIMATE_RXNORM_ID_TEXT,C.URL_APPROXIMATE_RXNORM_CONVERT_DRUG_NAME_TO_RXCUI);

		if (m_rxcui != null)
			m_drugNameToRXCUICache.put(commercialDrugName, m_rxcui);

		return m_rxcui;
	}

	private void extractRXCUI(String commercialDrugName, String xpath, String url) {
		XMLReader myReader = null;
		try {
			myReader = XMLReaderFactory.createXMLReader();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		XPathXMLHandler handler=new XPathXMLHandler()
		{			

			@Override
			public void findXpathNode(SAXXPath xpath, Object node) {
				m_rxcui = ((TextWrapper)node).getNodeValue();

			}
		};
		try {
			handler.setXPaths(XPathXMLHandler.toXPaths(xpath));
		} catch (XPathSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		myReader.setContentHandler(handler);
		try {
			myReader.parse(new InputSource(new URL(url+URLEncoder.encode(commercialDrugName, "UTF-8")).openStream()));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Anomalyresult insertRecord(String datasetID, String diagnosisCode, String commercialDrugName, boolean returnIsAnomaly, int diagnosisCodeType, boolean loadFromDB){
		//		if (!isDrugNameValid(commercialDrugName))
		//			return false;
		if (loadFromDB)
			loadFromDB(datasetID);

		if (diagnosisCode.equals("0")){
			//log.info("diagnosisCode = "+diagnosisCode+" is invalid");
			return null;
		}

		if (diagnosisCodeType == C.DIAGNOSIS_CODE_TYPE_ICD9_GENERALIZED)
			diagnosisCode = getGeneralizedICD9Code(diagnosisCode);

		IncrementFrequencyIfCombinationExists(datasetID, diagnosisCode, commercialDrugName);	
		m_totalPrescriptionsCount++;

		saveToDB(datasetID);

		return checkAnomalyIfNeeded(datasetID,diagnosisCode,commercialDrugName, returnIsAnomaly);
	}

	private void loadFromDB(String dataSetID) {
		SessionFactory sessionFactory = HibernateUtil.getSessionFactory();  
		org.hibernate.Session session = sessionFactory.openSession();  
		session.beginTransaction();  

		Criteria cr = session.createCriteria(Diagnosticvsdrugfrequency.class);
		cr.add(Restrictions.eq("datasetId", dataSetID));
		//List<Diagnosticvsdrugfrequency> diagnosticvsdrugfrequencyList = cr.list();
		ScrollableResults scrollableResults = cr.scroll(ScrollMode.FORWARD_ONLY);

		while (scrollableResults.next()) {			
			Diagnosticvsdrugfrequency row = (Diagnosticvsdrugfrequency) scrollableResults.get()[0];
			m_diagnosticVSDrugFreq.put(row.getHashkey(),new MutableLong(row.getValue()));
		}

		session.flush();
		session.getTransaction().commit();
		session.close();

	}

	public void saveToDB(String datasetID) {
		if (!m_diagnosticVSDrugFreq.isEmpty()){
			SessionFactory sessionFactory = HibernateUtil.getSessionFactory();  
			org.hibernate.Session session = sessionFactory.openSession();  
			session.beginTransaction();  

			session.createSQLQuery("truncate table diagnosticvsdrugfrequency").executeUpdate();
			saveDiagnosticvsdrugfrequencyToDB(session, datasetID);
			 
			session.flush();
			session.getTransaction().commit();
			session.close();  
		}
	}

	private void saveDiagnosticvsdrugfrequencyToDB(org.hibernate.Session session, String datasetID) {
		for (String key:getDiagnosticVSDrugFreq(datasetID).keySet()){
			Diagnosticvsdrugfrequency diagnosticvsdrugfrequency = new Diagnosticvsdrugfrequency();
			diagnosticvsdrugfrequency.setDatasetId(datasetID);
			diagnosticvsdrugfrequency.setHashkey(key);
			long value = 0;

			value = getDiagnosticVSDrugFreq(datasetID).get(key).get();

			diagnosticvsdrugfrequency.setValue(value);			
			session.persist(diagnosticvsdrugfrequency);
		}
	}

	public ArrayList<Anomalyresult> getAnomaliesByCommercialDrugName(String dataSetID, boolean saveAnomaliesToDB){
		loadFromDB(dataSetID);

		ArrayList<Anomalyresult> anomalies = new ArrayList<Anomalyresult>();

		for (String key: getDiagnosticVSDrugFreq(dataSetID).keySet()){
			Anomalyresult anomalyData;
			if ((anomalyData = RiskEngineAlgorithms.checkAnomalyA(dataSetID, key,getDiagnosticVSDrugFreq(dataSetID),m_fieldFreq,m_riskPercentageThreshold)) != null)
				anomalies.add(anomalyData);						
		}

		m_anomalySize = anomalies.size();
		
		if (saveAnomaliesToDB){
			long startTime = System.currentTimeMillis();
			Util.exportAnomaliesToDB(dataSetID, anomalies, this);
			long timeTakenTosaveAnomaliesToDB = System.currentTimeMillis()-startTime;
			log.info("timeTakenTosaveAnomaliesToDB in milliseconds="+timeTakenTosaveAnomaliesToDB);
		}
		
		saveCache();

		return anomalies;
	}



	public ArrayList<Anomalyresult> getAnomaliesByIngredients(String dataSetID){
		ArrayList<Anomalyresult> anomalies = new ArrayList<Anomalyresult>();

		for (String key: m_diagnosticVSIngredientsFreq.keySet()){
			Anomalyresult anomalyValue;
			if ((anomalyValue=RiskEngineAlgorithms.checkAnomalyA(dataSetID ,key,m_diagnosticVSIngredientsFreq,m_fieldFreq,m_riskPercentageThreshold))!=null)
				anomalies.add(anomalyValue);			
		}

		m_anomalySize = anomalies.size();
		return anomalies;
	}

	public int getLastCalculatedAnomalySize() {
		return m_anomalySize;
	}

	private Anomalyresult checkAnomalyIfNeeded(String dataSetID, String diagnosisCode, String[] drugIngredients, boolean returnIsAnomaly) {
		Anomalyresult isAnomaly = null;
		if (returnIsAnomaly)
			isAnomaly = checkAnomaly(dataSetID, diagnosisCode,drugIngredients);
		return isAnomaly;
	}

	private Anomalyresult checkAnomaly(String dataSetID, String diagnosisCode, String[] drugIngredients) {
		String key = generateKey(diagnosisCode, drugIngredients);

		return RiskEngineAlgorithms.checkAnomalyA(dataSetID, key, m_diagnosticVSIngredientsFreq, m_fieldFreq, m_riskPercentageThreshold);		
	}

	private void IncrementFrequencyIfCombinationExists(String diagnosisCode, String[] drugIngredients) {
		String DiagnosticVSCommercialDrugNameKey = generateKey(diagnosisCode,drugIngredients);
		MutableLong count = m_diagnosticVSIngredientsFreq.get(DiagnosticVSCommercialDrugNameKey);
		if (count == null) {
			m_diagnosticVSIngredientsFreq.put(DiagnosticVSCommercialDrugNameKey, new MutableLong());
		}
		else {
			count.increment();
		}

		count = m_fieldFreq.get(diagnosisCode);
		if (count == null) {
			m_fieldFreq.put(diagnosisCode, new MutableLong());
		}
		else {
			count.increment();
		}

		for (String ingredient: drugIngredients){
			count = m_fieldFreq.get(ingredient);
			if (count == null) {
				m_fieldFreq.put(ingredient, new MutableLong());
			}
			else {
				count.increment();
			}
		}



	}

	public String generateKey(String diagnosisCode, String[] drugIngredients) {		
		StringBuffer key = new StringBuffer();
		key.append(diagnosisCode);
		for (String s: drugIngredients)
		{           
			key.append(C.KEY_DELIMITER);
			key.append(s);
		}
		return key.toString();
	}

	private Anomalyresult checkAnomalyIfNeeded(String datasetID, String diagnosisCode,	String commercialDrugName, boolean returnIsAnomaly) {
		Anomalyresult isAnomaly = null;

		if (returnIsAnomaly)
			isAnomaly = checkAnomaly(datasetID, diagnosisCode,commercialDrugName);
		return isAnomaly;
	}

	private void IncrementFrequencyIfCombinationExists(String datasetID, String diagnosisCode, String commercialDrugName) {
		String diagnosticVSCommercialDrugNameKey = generateKey(diagnosisCode,commercialDrugName);

		MutableLong count = getDiagnosticVSDrugFreq(datasetID).get(diagnosticVSCommercialDrugNameKey);
		if (count == null) {
			getDiagnosticVSDrugFreq(datasetID).put(diagnosticVSCommercialDrugNameKey, new MutableLong());
		}
		else {
			count.increment();
		}

		count = m_fieldFreq.get(diagnosisCode);
		if (count == null) {
			m_fieldFreq.put(diagnosisCode, new MutableLong());
		}
		else {
			count.increment();
		}

		count = m_fieldFreq.get(commercialDrugName);
		if (count == null) {
			m_fieldFreq.put(commercialDrugName, new MutableLong());
		}
		else {
			count.increment();
		}		
	}

	//	private boolean isDrugNameValid(String commercialDrugName) {
	//		String rxcui = getRXCUIByDrugName(commercialDrugName);
	//		if (!StringUtil.notEmpty(rxcui)){
	//			//log.info("Drug name \""+commercialDrugName+"\" is Invalid" );
	//			m_invalidDrugNamesArray.add(commercialDrugName);
	//			return false;
	//		}
	//		return true;
	//	}

	public Set<String> getInvalidDrugNamesArray() {
		return m_invalidDrugNamesArray;
	}

	public String generateKey(String diagnosisCode, String commercialDrugName) {
		return diagnosisCode+C.KEY_DELIMITER+commercialDrugName;
	}

	public Anomalyresult checkAnomaly(String datasetID, String diagnosisCode,	String commercialDrugName) {
		String key = generateKey(diagnosisCode,commercialDrugName);
		return RiskEngineAlgorithms.checkAnomalyA(datasetID, key,getDiagnosticVSDrugFreq(datasetID), m_fieldFreq, m_riskPercentageThreshold);
	}	

	private String checkAnomalyAWithJust2Dimensions(String key, Map<String, MutableLong> hashFrequency) {
		double instanceFrequency = hashFrequency.get(key).get();

		String diagnosisName = key.split(C.KEY_DELIMITER)[0];
		double diagnosisFrequency = m_fieldFreq.get(diagnosisName).get(); 
		String drugName = key.split(C.KEY_DELIMITER)[1];
		double drugFrequency = m_fieldFreq.get(drugName).get();

		if ((instanceFrequency/diagnosisFrequency*instanceFrequency/drugFrequency)<m_riskPercentageThreshold){			
			String reason = "algorithmAWithJust2Dimensions";
			return key+C.KEY_DELIMITER+instanceFrequency+C.KEY_DELIMITER+diagnosisName+C.KEY_DELIMITER+diagnosisFrequency+C.KEY_DELIMITER+drugName+C.KEY_DELIMITER+drugFrequency+C.KEY_DELIMITER+reason;
		}
		return null;
	}

	private String checkAnomalyB(String key, Map<String, MutableLong> hashFrequency) {
		double instanceFrequency = hashFrequency.get(key).get();

		String diagnosisName = key.split(C.KEY_DELIMITER)[0];
		double diagnosisFrequency = m_fieldFreq.get(diagnosisName).get(); 
		String drugName = key.split(C.KEY_DELIMITER)[1];
		double drugFrequency = m_fieldFreq.get(drugName).get();

		if ((instanceFrequency/m_totalPrescriptionsCount)<m_riskPercentageThreshold){			
			String reason = "algorithmB";
			return key+C.KEY_DELIMITER+instanceFrequency+C.KEY_DELIMITER+diagnosisName+C.KEY_DELIMITER+diagnosisFrequency+C.KEY_DELIMITER+drugName+C.KEY_DELIMITER+drugFrequency+reason;
		}
		return null;
	}

	private String checkAnomalyCPlusD(String key, Map<String, MutableLong> hashFrequency) {
		long instanceFrequency = hashFrequency.get(key).get();
		//		if (m_totalPrescriptionsCount>0)
		//			if (((double)instanceFrequency / m_totalPrescriptionsCount) < m_riskPercentageThreshold)
		//				return true;

		for (String fieldName: key.split(C.KEY_DELIMITER)) {
			long fieldFrequency = m_fieldFreq.get(fieldName).get();
			if ((((double)instanceFrequency / fieldFrequency) < m_riskPercentageThreshold) && (instanceFrequency<=C.INSTANCE_FREQUENCY_COUNT_THRESHOLD)){

				String reason = "The Drug "+fieldName+" was prescribed only in "+instanceFrequency+" out of "+fieldFrequency+" diagnoses of "+getICD9shortDescription(key.split(C.KEY_DELIMITER)[0])+" - which is lower than the "+m_riskPercentageThreshold*100+"% that was set as the risk percentage threshold"; 

				return key+C.KEY_DELIMITER+instanceFrequency+C.KEY_DELIMITER+fieldName+C.KEY_DELIMITER+fieldFrequency+C.KEY_DELIMITER+reason;
			}
		}

		return null;	
	}




	private String getICD9shortDescription(String icd9Code) {
		String shortDescription = getICD9Description(icd9Code).split(C.CSV_DELIMITER_STRING)[0];
		if (shortDescription!=null)
			return shortDescription;
		else
			return "<Diagnosis Name Missing>";
	}

	//	private String checkAnomalyWithIngredients(String key) {
	//		long instanceFrequency = m_diagnosticVSIngredientsFreq.get(key).get();
	//		//		if (m_totalPrescriptionsCount>0)
	//		//			if ((double)instanceFrequency / m_totalPrescriptionsCount < m_riskPercentageThreshold) 
	//		//				return true;
	//
	//		for (String fieldName: key.split(KEY_DELIMITER)) {
	//			long fieldFrequency = fieldFreq.get(fieldName).get();
	//			if (((double)instanceFrequency / fieldFrequency < m_riskPercentageThreshold) && (instanceFrequency<=INSTANCE_FREQUENCY_COUNT_THRESHOLD))
	//				return key+C.KEY_DELIMITER+instanceFrequency+C.KEY_DELIMITER+fieldName+C.KEY_DELIMITER+fieldFrequency;
	//		}
	//
	//		return null;
	//	}

	public boolean isExists(String datasetID, String diagnosisCode, String commercialDrugName, boolean convertDrugToIngredients) {
		if (convertDrugToIngredients)
			try {
				return isExists(diagnosisCode, getDrugIngredientsByCommercialDrugName(commercialDrugName));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} 
		else
			return getDiagnosticVSDrugFreq(datasetID).get(generateKey(diagnosisCode, commercialDrugName))!=null;
	}

	public boolean isExists(String datasetID, String diagnosisCode, String commercialDrugName) {
		return isExists(datasetID, diagnosisCode, commercialDrugName, false); 
	}

	public void deleteRecord(String datasetID, String diagnosisCode, String commercialDrugName) {
		getDiagnosticVSDrugFreq(datasetID).remove(generateKey(diagnosisCode, commercialDrugName));		
	}

	public boolean isExists(String diagnosisCode, String[] Ingredients) {
		return m_diagnosticVSIngredientsFreq.get(generateKey(diagnosisCode, Ingredients))!=null;
	}

	public void deleteRecord(String datasetID, String diagnosisCode, String[] Ingredients) {
		getDiagnosticVSDrugFreq(datasetID).remove(generateKey(diagnosisCode, Ingredients));		
	}

	public void clearAllRecords(String datasetID) {
		getDiagnosticVSDrugFreq(datasetID).clear();		
		m_diagnosticVSIngredientsFreq.clear();
		m_totalPrescriptionsCount = 0;	
		m_fieldFreq.clear();
		clearFromDB();		
	}

	private void clearFromDB() {
		SessionFactory sessionFactory = HibernateUtil.getSessionFactory();  
		final org.hibernate.Session session = sessionFactory.openSession();  
		session.beginTransaction(); 

		session.createSQLQuery("delete from diagnosticvsdrugfrequency").executeUpdate();

		session.getTransaction().commit();
		session.flush();
		session.close();
	}

	public long getDataSetSize() {
		return m_totalPrescriptionsCount;
	}

	public Entry<String, MutableLong> getTopAnomalyForDiagnosticVSDrug(String datasetID) {
		MyComparator comp = new MyComparator(getDiagnosticVSDrugFreq(datasetID));
		TreeMap<String, MutableLong> treeMap = new TreeMap<String, MutableLong>(comp);
		treeMap.putAll(getDiagnosticVSDrugFreq(datasetID));

		return treeMap.firstEntry();
	}


	public class MyComparator implements Comparator<Object> {

		Map<String, MutableLong> theMapToSort;

		public MyComparator(Map<String, MutableLong> theMapToSort) {
			this.theMapToSort = theMapToSort;
		}

		public int compare(Object key1, Object key2) {
			MutableLong val1 = (MutableLong) theMapToSort.get(key1);
			MutableLong val2 = (MutableLong) theMapToSort.get(key2);
			if (val1.get() < val2.get()) {
				return -1;
			} else {
				return 1;
			}
		}
	}


	public void deleteRecord(String datasetID,String diagnosisCode, String commercialDrugName,
			boolean convertCommercialDrugNameToIngredients) {
		if (convertCommercialDrugNameToIngredients)
			try {
				deleteRecord(datasetID,diagnosisCode, getDrugIngredientsByCommercialDrugName(commercialDrugName));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		else 
			deleteRecord(datasetID,diagnosisCode, commercialDrugName);
	}

	public double getAnomalyPercentage() {
		if (getDataSetSize()>0)
			return (double)getLastCalculatedAnomalySize()/getDataSetSize()*100;

		return 0;
	}

	public String getDrugDescription(String drugName){	

		String description = m_drugDescriptionCache.get(drugName);
		if (description!=null)
			return description;
		if (m_invalidDrugNamesArray.contains(drugName))
			return null;

		Document doc = null;
		try {
			doc = Jsoup.connect(C.URL_SEARCH_DRUG_NAME+drugName).userAgent("Mozilla/5.0").timeout(5000).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (doc!=null){
			Element element = doc.select(".search-result-desc").first();
			if (element!=null)
				description = element.text();
		}

		if (description == null)
			m_invalidDrugNamesArray.add(drugName);
		else
			m_drugDescriptionCache.put(drugName, description);
		//log.info(drugName+" -  "+description);

		return description;
	}

	public String getICD9Description(String icd9Code){	

		String description = m_icd9DescriptionCache.get(icd9Code);
		if (description!=null)
			return description;

		Document doc = null;
		try {
			description = getCodeDescriptionFromHTML(icd9Code, description,".00");	
			if (description==null || description.startsWith("Map ICD-9-CM"))
				description = getCodeDescriptionFromHTML(icd9Code, description,".0");
		} catch (Exception e) {
			e.printStackTrace();			
		}

		m_icd9DescriptionCache.put(icd9Code, description);
		//log.info(icd9Code+" - "+description);

		return description;
	}

	private String getCodeDescriptionFromHTML(String icd9Code,
			String description, String suffix) throws IOException {

		Document doc;

		doc = Jsoup.connect(C.URL_SEARCH_ICD_9+icd9Code+suffix).userAgent("Mozilla/5.0").timeout(5000).get();

		try {
			description = doc.select(".div-table-col").get(2).text();
			if (description.equals(StringUtil.EMPTY_STRING))
				description = retryExtractDatawith2ndLocationOnHTMLPage(doc);				
		} catch (Exception e) {
			e.printStackTrace();
			description = retryExtractDatawith2ndLocationOnHTMLPage(doc);
		}

		return description;
	}

	private String retryExtractDatawith2ndLocationOnHTMLPage(Document doc) {
		String description = null;
		try {			
			description = doc.select(".h2link").get(1).text();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return description;
	}

	public void saveCache() {
		saveCache(C.CACHE_FOLDER_FILE_PATH);		
	}

	public Pair<List<Long>, List<Long>> calculateStats(String datasetID)
	{
		Collection<MutableLong> list = getDiagnosticVSDrugFreq(datasetID).values();
		double[] data = new double[list.size()];
		for(int i = 0; i < list.size(); i++)
			data[i] = ((MutableLong) getDiagnosticVSDrugFreq(datasetID).values().toArray()[i]).get();
		DescriptiveStatistics dStats = new DescriptiveStatistics(data);

		List<Long> summary = new ArrayList<Long>(5);
		summary.add( (long) dStats.getMin()); //Minimum
		summary.add( (long) dStats.getPercentile(25)); //Lower Quartile (Q1)
		summary.add( (long) dStats.getPercentile(50)); //Middle Quartile (Median - Q2)
		summary.add( (long) dStats.getPercentile(75)); //High Quartile (Q3)
		summary.add( (long) dStats.getMax()); //Maxiumum

		List<Long> outliers = new ArrayList<Long>();
		if(list.size() > 5 && dStats.getStandardDeviation() > 0) //Only remove outliers if relatively normal
		{
			double mean = dStats.getMean();
			logger.info("mean="+mean);
			double stDev = dStats.getStandardDeviation();
			NormalDistribution normalDistribution = new NormalDistribution(mean, stDev);

			Iterator<MutableLong> listIterator = list.iterator();
			double significanceLevel = .50 / list.size();
			while(listIterator.hasNext())
			{
				MutableLong num = listIterator.next();
				double pValue = normalDistribution.cumulativeProbability(num.get());
				if(pValue < significanceLevel) //Chauvenet's Criterion for Outliers
				{
					outliers.add(num.get());
					listIterator.remove();
				}
			}

			if(list.size() != dStats.getN()) //If and only if outliers have been removed
			{
				double[] significantData = new double[list.size()];
				for(int i = 0; i < list.size(); i++)
					significantData[i] = ((MutableLong)list.toArray()[i]).get();
				dStats = new DescriptiveStatistics(significantData);
				summary.set(0, (long) dStats.getMin());
				summary.set(4, (long) dStats.getMax());
			}
		}

		return new Pair<List<Long>,List<Long>>(summary, outliers);
	}

	public Anomalyresult insertRecord(String datasetID, String diagnosticCode, String commercialDrugName1, boolean returnIsAnomaly) {
		return insertRecord(datasetID, diagnosticCode, commercialDrugName1, returnIsAnomaly, C.DIAGNOSIS_CODE_TYPE_ICD9_GENERALIZED);		
	}

	public Map<String, MutableLong> getDiagnosticVSDrugFreq(String dataSetID) {
		if (m_diagnosticVSDrugFreq==null)
			loadFromDB(dataSetID);
		return m_diagnosticVSDrugFreq;
	}

	public void setDiagnosticVSDrugFreq(
			Map<String, MutableLong> m_diagnosticVSDrugFreq) {
		this.m_diagnosticVSDrugFreq = m_diagnosticVSDrugFreq;
	}

	public boolean importInputDataSetFile(String filePath)
	{
		SessionFactory sessionFactory = HibernateUtil.getSessionFactory();  
		org.hibernate.Session session = sessionFactory.openSession();  
		session.beginTransaction(); 

		session.createSQLQuery("LOAD DATA INFILE :file INTO TABLE "+C.DB_TABLE_INPUT_DATASET+" COLUMNS TERMINATED BY ','"
				+" OPTIONALLY ENCLOSED BY '\"'"
				+" ESCAPED BY '\"'"
				+" LINES TERMINATED BY '\n'"
				+" IGNORE 1 LINES;")
				.setString("file", filePath)
				.executeUpdate();

		session.flush();
		session.close();
		return true;
	}

	public boolean importInputDatasetCSVFromInputStream(
			final String fileName, InputStream fileInputStream) {
		
		deleteExistingDataSetRecords(fileName);
		
		SessionFactory sessionFactory = HibernateUtil.getSessionFactory();  
		final org.hibernate.Session session = sessionFactory.openSession();  
		session.beginTransaction();

		CSV csv = CSV
				.separator(',')  // delimiter of fields
				.quote('"')// quote character
				.create();       // new instance is immutable

		csv.read(fileInputStream, new CSVReadProc() {
			public void procRow(int rowIndex, String... values) {
				if (rowIndex%10000==0)
					log.info("importing from CSV : "+rowIndex + ": " + Arrays.asList(values));
				Inputdataset inputDataset = new Inputdataset();
				int i=0;
				inputDataset.setDatasetId(fileName);
				inputDataset.setExternalId(values[i++]);
				inputDataset.setDiagnosis1(values[i++]);
				inputDataset.setDiagnosis2(values[i++]);
				inputDataset.setDiagnosis3(values[i++]);
				inputDataset.setDrugName(values[i++]);
				session.persist(inputDataset);
				
//				if ( rowIndex % C.COMMIT_BATCH_SIZE == 0 ) { 
//			        //flush a batch of inserts and release memory:
//			        session.flush();
//			        session.clear();
//			    }
			}
		});

		session.flush();
		session.getTransaction().commit();		
		session.close();
		return true;
	}

	private void deleteExistingDataSetRecords(String datasetIdParameter) {
		
		SessionFactory sessionFactory = HibernateUtil.getSessionFactory();  
		final org.hibernate.Session session = sessionFactory.openSession();  
		session.beginTransaction();
		//String hqlDelete = "delete inputdataset i where i.datasetId = :datasetIdParameter";
		String hqlDelete = "delete Inputdataset where datasetId = :datasetIdParameter";
		int deletedEntities = session.createQuery( hqlDelete )
				.setString( "datasetIdParameter", datasetIdParameter )
				.executeUpdate();
		session.getTransaction().commit();		
	}

	public boolean runCalculationOnDBWithSpecificDataSetID(String datasetID) {
		clearAllRecords(datasetID);

		SessionFactory sessionFactory = HibernateUtil.getSessionFactory();  
		final org.hibernate.Session session = sessionFactory.openSession();  
		session.beginTransaction();

		Criteria cr = session.createCriteria(Inputdataset.class);
		cr.add(Restrictions.eq("datasetId", datasetID));
		//List<Inputdataset> list = cr.list();
		ScrollableResults scrollableResults = cr.scroll(ScrollMode.FORWARD_ONLY);

		while (scrollableResults.next()) {			
			Inputdataset row = (Inputdataset) scrollableResults.get()[0];
			String[] diagnosisCodes = {row.getDiagnosis1(),row.getDiagnosis2(),row.getDiagnosis3()};
			insertRecord(datasetID,diagnosisCodes, row.getDrugName(), false);			
		}

		session.flush();
		session.close();

		saveToDB(datasetID);
		return true;
	}

	public void loadCache() {
		
		loadCache(C.CACHE_FOLDER_FILE_PATH);
		
	}


}
