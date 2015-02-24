package Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gigmed.riskengine.common.C;
import com.gigmed.riskengine.common.Util;
import com.gigmed.riskengine.dao.Anomalyresult;
import com.gigmed.riskengine.repository.*;

import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVReadProc;

public class TestInsertRecordForCommercialDrugName {
	
	private static final String DATABASE_FOLDER_PATH = "C:\\GigMed\\Databases\\";	
	private static final String ANOMALIES_OUTPUT_FOLDER_PATH = DATABASE_FOLDER_PATH+"WorkersComp\\Anomalies\\";
	private static final String BY_DRUG_NAME_CSV = "ByDrugName.csv";
	private static final String BY_INGREDIENT_CSV = "ByIngredient.csv";
	private static final int NUMBER_OF_DRUGS_PER_DIAGNOSTIC = 30;
	private static final int NUMBER_OF_DIAGNOSTICS = 14000;
	private static final int NUMBER_OF_PRESCRIPTIONS = 1000;
	
	String diagnosticCode1 = "111";
	String diagnosticCode2 = "222";
	String diagnosticCode3 = "333";
	String commercialDrugName1 = "aspirin";
	String commercialDrugName2 = "celebrex";
	String commercialDrugName3 = "advil";
	String invalidDrugNameForTest = "chemotherapy";
	RiskEngineRepository engine;
	private Logger logger; 


	@Before
	public void setUp() throws Exception {	 
	    engine = new RiskEngineRepository(RISK_PERCENTAGE_THRESHOLD);	    
	}	

	@After
	public void tearDown() throws Exception {		
		engine = null;
	}

	@Test
	public void testInsertRecordDrugNameWithoutAnomalyCheck() {				
		engine.insertRecord("test",diagnosticCode1, commercialDrugName1, false);
		assertTrue(engine.isExists("test",diagnosticCode1, commercialDrugName1));
		assertEquals(1,engine.getDataSetSize());
		engine.deleteRecord("test",diagnosticCode1, commercialDrugName1);
	}

	@Test
	public void testInsertRecordDrugNameWithAnomalyCheck() {				
		assertFalse(engine.insertRecord("test",diagnosticCode1, commercialDrugName1, true)!=null);
		assertTrue(engine.isExists("test",diagnosticCode1, commercialDrugName1));
		assertEquals(1,engine.getDataSetSize());
		engine.deleteRecord("test",diagnosticCode1, commercialDrugName1,true);	
	}

	@Test
	public void testInsertRecordDrugNameConvertedToIngredientsWithAnomalyCheck() {				
		assertFalse(engine.insertRecord("test",diagnosticCode1, commercialDrugName1, true, true)!=null);
		assertTrue(engine.isExists("test",diagnosticCode1, commercialDrugName1, true));
		assertEquals(1,engine.getDataSetSize());
		engine.deleteRecord("test",diagnosticCode1, commercialDrugName1, true);	
	}

	@Test
	public void testInsertRecordDrugNameConvertedToIngredientsWithInvalidDrugNameAndAnomalyCheck() {				
		assertFalse(engine.insertRecord("test",diagnosticCode1, invalidDrugNameForTest, true, true)!=null);
		assertEquals(1,engine.getDataSetSize());
		engine.deleteRecord("test",diagnosticCode1, invalidDrugNameForTest, true);	
	}

	@Test
	public void testFailedInsertRecordDrugName() {
		engine.deleteRecord("test",diagnosticCode1, commercialDrugName1);		
		assertEquals(0,engine.getDataSetSize());
		assertFalse(engine.isExists("test",diagnosticCode1, commercialDrugName1));		
	}

	@Test
	public void testInsertMultipleRecordsWithOneAnomaly() {
		for (int i=0 ; i<NUMBER_OF_PRESCRIPTIONS; i++){
			engine.insertRecord("test",diagnosticCode1, commercialDrugName1, false);			
		}

		engine.insertRecord("test",diagnosticCode1, commercialDrugName2, false);

		ArrayList<String> expectedValue = new ArrayList<String>();
		expectedValue.add(engine.generateKey(diagnosticCode1,engine.generateKey(commercialDrugName2,"1")));

		assertEquals(expectedValue.toString() , engine.getAnomaliesByCommercialDrugName("test",false).toString());
		assertEquals(NUMBER_OF_PRESCRIPTIONS+1,engine.getDataSetSize());

		engine.clearAllRecords("test");
	}

	@Test
	public void testInsertMultipleRecordsWithTwoAnomalies() {
		engine.setRiskPercentageThreshold(0.01);
		
		for (int i=0 ; i<NUMBER_OF_PRESCRIPTIONS; i++){
			engine.insertRecord("test",diagnosticCode1, commercialDrugName1, true);			
		}

		engine.insertRecord("test",diagnosticCode1, commercialDrugName2, true);

		for (int i=0 ; i<NUMBER_OF_PRESCRIPTIONS; i++){
			engine.insertRecord("test",diagnosticCode1, commercialDrugName1, true);			
		}

		engine.insertRecord("test",diagnosticCode1, commercialDrugName3, true);


		ArrayList<String> expectedValue = new ArrayList<String>();
		expectedValue.add(engine.generateKey(diagnosticCode1,engine.generateKey(commercialDrugName3,"1")));
		expectedValue.add(engine.generateKey(diagnosticCode1,engine.generateKey(commercialDrugName2,"1")));

		ArrayList<Anomalyresult> anomalies = engine.getAnomaliesByCommercialDrugName("test",false);

		//		logger.info("expected");
		//		logger.info(expectedValue);
		//		logger.info("anomalies");		
		//		logger.info(anomalies);

		assertEquals(expectedValue.toString() , anomalies.toString());

		engine.clearAllRecords("test");
	}

	@Test
	public void testInsertMultipleRecordsWith50PercentSplit() {
		for (int i=0 ; i<NUMBER_OF_PRESCRIPTIONS; i++){
			engine.insertRecord("test",diagnosticCode1, commercialDrugName1, true);			
		}		

		for (int i=0 ; i<NUMBER_OF_PRESCRIPTIONS; i++){
			engine.insertRecord("test",diagnosticCode1, commercialDrugName2, true);			
		}		

		ArrayList<String> expectedValue = new ArrayList<String>();

		assertEquals(expectedValue.toString() , engine.getAnomaliesByCommercialDrugName("test",false).toString());

		engine.clearAllRecords("test");		
	}

	@Test
	public void testInsertMultipleRecordsWith1PercentOfDrug2() {

		RiskEngineRepository engine1PercentRisk = new RiskEngineRepository(0.01);

		for (int i=0 ; i<99; i++){
			engine1PercentRisk.insertRecord("test",diagnosticCode1, commercialDrugName1, true);			
		}		

		for (int i=0 ; i<1; i++){
			engine1PercentRisk.insertRecord("test",diagnosticCode1, commercialDrugName2, true);			
		}		

		ArrayList<String> expectedValue = new ArrayList<String>();

		assertEquals(expectedValue.toString() , engine1PercentRisk.getAnomaliesByCommercialDrugName("test",false).toString());

		engine1PercentRisk.clearAllRecords("test");		
	}

	@Test
	public void testInsertMultipleRecordsWithlessThan1PercentOfDrug2() {
		testInsertMultipleRecordsWithlessThan1PercentOfDrug2(false);
	}

	@Test
	public void testInsertMultipleRecordsConvertedToIngredientsWithlessThan1PercentOfDrug2() {
		testInsertMultipleRecordsWithlessThan1PercentOfDrug2(true);
	}


	public void testInsertMultipleRecordsWithlessThan1PercentOfDrug2(boolean convertDrugNameToIngredients) {
		RiskEngineRepository engine1PercentRisk = new RiskEngineRepository(0.01);

		for (int i=0 ; i<100; i++){
			engine1PercentRisk.insertRecord("test",diagnosticCode1, commercialDrugName1, true, convertDrugNameToIngredients);			
		}		

		engine1PercentRisk.insertRecord("test",diagnosticCode1, commercialDrugName2, true, convertDrugNameToIngredients);			


		ArrayList<String> expectedValue = new ArrayList<String>();
		if (convertDrugNameToIngredients)
			expectedValue.add(engine1PercentRisk.generateKey(engine1PercentRisk.generateKey(diagnosticCode1,engine1PercentRisk.getDrugIngredientsByCommercialDrugName(commercialDrugName2)),"1"));
		else
			expectedValue.add(engine1PercentRisk.generateKey(diagnosticCode1,engine1PercentRisk.generateKey(commercialDrugName2,"1")));

		ArrayList<Anomalyresult> anomalies;
		if (convertDrugNameToIngredients)
			anomalies = engine1PercentRisk.getAnomaliesByIngredients(C.DATA_SET_ID_CMD);
		else			
			anomalies = engine1PercentRisk.getAnomaliesByCommercialDrugName("test",false);

		//		logger.info("expected");
		//		logger.info(expectedValue);
		//		logger.info("anomalies");		
		//		logger.info(anomalies);

		assertEquals(expectedValue.toString() , anomalies.toString());

		engine1PercentRisk.clearAllRecords("test");		
	}

//	@Test
//	public void testInsertMultipleRandomizedRecordsWithlessThan1PercentOfDrug1() {
//
//		Random randomGenerator = new Random(System.currentTimeMillis());
//		for (int i=0 ; i<NUMBER_OF_PRESCRIPTIONS; i++) {
//			int randomDiagnosticCode = randomGenerator.nextInt(NUMBER_OF_DIAGNOSTICS);
//			randomGenerator = new Random();
//			int randomCommercialDrugName = randomGenerator.nextInt(NUMBER_OF_DRUGS_PER_DIAGNOSTIC);
//			engine.insertRecord("test",String.valueOf(randomDiagnosticCode), String.valueOf(randomCommercialDrugName), true);
//		}
//
//		//logger.info(engine.getAnomaliesByCommercialDrugName("test",false));
//		engine.insertRecord("test",diagnosticCode, commercialDrugName1, true);							
//
//		ArrayList<String> expectedValue = new ArrayList<String>();
//		expectedValue.add(engine.generateKey(diagnosticCode,engine.generateKey(commercialDrugName1,"1")));
//
//		ArrayList<String> anomalies = engine.getAnomaliesByCommercialDrugName("test",false);
//
//		engine.getTopAnomalyForDiagnosticVSDrug();
//		//		logger.info("top anomaly="+topAnomaly.getKey()+" number of intances="+topAnomaly.getValue().get());
//		//		logger.info("DataSetSize="+engine.getDataSetSize());
//		assertEquals(expectedValue.toString() , anomalies.toString());
//
//		engine.clearAllRecords("test");	
//	}	

	private static final double RISK_PERCENTAGE_THRESHOLD = 0.001;
	
	private static final String ANOMALIES_OUTPUT_CSV_FILE_NAME = "PatientWorkersCompAnomalies";
	@Test
	public void testImportRealDataFromWorkersCompSmallPatientCSVFileByDrugName() {
		logger = Util.getInitializedLogger();
		engine = new RiskEngineRepository(0.1);
		testImportRealDataFromWorkersCompCSVFile(RiskEngineRepository.TEST_BY_DRUG_NAME,"Small");
	}

	@Test
	public void testImportRealDataFromWorkersCompSmallPatientCSVFileByIngredient() {
		engine = new RiskEngineRepository(0.01);
		testImportRealDataFromWorkersCompCSVFile(RiskEngineRepository.TEST_BY_INGREDIENT,"Small");
	}
	
	@Test
	public void testImportRealDataFromWorkersCompInPatientCSVFileByDrugName() {
		engine = new RiskEngineRepository(0.00001);
		logger = Util.getInitializedLogger();
		testImportRealDataFromWorkersCompCSVFile(RiskEngineRepository.TEST_BY_DRUG_NAME,"In");
	}

	@Test
	public void testImportRealDataFromWorkersCompInPatientCSVFileByIngredient() {
		engine = new RiskEngineRepository(0.00001);
		testImportRealDataFromWorkersCompCSVFile(RiskEngineRepository.TEST_BY_INGREDIENT,"In");
	}

	@Test
	public void testImportRealDataFromWorkersCompOutPatinetCSVFileByDrugName() {
		engine = new RiskEngineRepository(0.0000005);
		testImportRealDataFromWorkersCompCSVFile(RiskEngineRepository.TEST_BY_DRUG_NAME,"Out");
		engine.saveCache();
	}

	@Test
	public void testImportRealDataFromWorkersCompOutPatientCSVFileByIngredient() {
		engine = new RiskEngineRepository(0.0000005);
		testImportRealDataFromWorkersCompCSVFile(RiskEngineRepository.TEST_BY_INGREDIENT,"Out");
		engine.saveCache();
		sendEmail();	
	}
	
	private void sendEmail() {
		try {
			Util.sendEmail("ari@volcoff.com", "Anomaly detection process is done", "All tests completed");
		} catch (AddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void testImportRealDataFromWorkersCompCSVFile(final String testType, String databasePrefix) {

		CSV csv = CSV
				.separator(',')  // delimiter of fields
				.quote('"')      // quote character
				.create();       // new instance is immutable

		long timeBeforeImport = System.currentTimeMillis();

		csv.read(DATABASE_FOLDER_PATH+"WorkersComp\\NormalizedData\\"+databasePrefix+"PatientWorkersCompNormalizedDataSet.csv", new CSVReadProc() {
			public void procRow(int rowIndex, String... values) {
				//logger.info(rowIndex + ": " + Arrays.asList(values));
				String diagnosticCode = values[5];
				String drugName = values[3];
				String trackBackID = values[0]+C.KEY_DELIMITER+values[2];

				if (testType.equals(RiskEngineRepository.TEST_BY_DRUG_NAME))
					engine.insertRecord("test",diagnosticCode, drugName, false, C.DIAGNOSIS_CODE_TYPE_ICD9_GENERALIZED);
				if (testType.equals(RiskEngineRepository.TEST_BY_INGREDIENT))
					engine.insertRecord("test",diagnosticCode, drugName, false, true, C.DIAGNOSIS_CODE_TYPE_ICD9_GENERALIZED);
			}
		});
		
		long timeAfterImport = System.currentTimeMillis();
		Util.logProcessingTimeSummary("Data Set import",timeBeforeImport, timeAfterImport, logger, engine);
		logger.info("Dataset record count= "+ engine.getDataSetSize());

//		long riskPercentageThreshold = 200/engine.getDataSetSize();
//		engine.setRiskPercentageThreshold(riskPercentageThreshold);
//		logger.info("riskPercentageThreshold="+riskPercentageThreshold);
		
		if (testType.equals(RiskEngineRepository.TEST_BY_DRUG_NAME)){
			long timeBeforeFindingAnomaliesByDrugName = System.currentTimeMillis();
			ArrayList<Anomalyresult> anomalies = engine.getAnomaliesByCommercialDrugName("test",false);
			long timeAfterFindingAnomaliesByDrugName = System.currentTimeMillis();
			Util.logProcessingTimeSummary("getAnomaliesByCommercialDrugName",timeBeforeFindingAnomaliesByDrugName, timeAfterFindingAnomaliesByDrugName, logger, engine);
			logger.info("We found these anomalies by Drug Name:"+anomalies.toString());
			Util.exportAnomaliesToFile(anomalies,ANOMALIES_OUTPUT_FOLDER_PATH+databasePrefix+ANOMALIES_OUTPUT_CSV_FILE_NAME+BY_DRUG_NAME_CSV, engine);
			Util.logCalculationSummary(logger, engine);
		}		


		if (testType.equals(RiskEngineRepository.TEST_BY_INGREDIENT)){

			long timeBeforeFindingAnomaliesByIngredients = System.currentTimeMillis();
			ArrayList<Anomalyresult> anomalies = engine.getAnomaliesByIngredients(C.DATA_SET_ID_CMD);
			long timeAfterFindingAnomaliesByIngredients = System.currentTimeMillis();
			Util.logProcessingTimeSummary("getAnomaliesByIngredients",timeBeforeFindingAnomaliesByIngredients, timeAfterFindingAnomaliesByIngredients, logger, engine);
			logger.info("We found these anomalies by Ingredients:"+anomalies.toString());
			Util.exportAnomaliesToFile(anomalies,ANOMALIES_OUTPUT_FOLDER_PATH+databasePrefix+ANOMALIES_OUTPUT_CSV_FILE_NAME+BY_INGREDIENT_CSV, engine);
			Util.logCalculationSummary(logger, engine);
			Set<String> invalidDrugNames = engine.getInvalidDrugNamesArray();
			logger.info("We found "+invalidDrugNames.size()+" invalid Drug Names = "+ invalidDrugNames);
		}

		engine.clearAllRecords("test");		
	}	

	private void wait1Second() {
		try {
		    Thread.sleep(1000);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
	}
	
	@Test
	public void testInsertRecordXDiagnosis1DrugNameWithAnomalyCheck() {
		String[] diagnosisCodes= {diagnosticCode1,diagnosticCode2,diagnosticCode3};
		assertFalse(engine.insertRecord("test",diagnosisCodes, commercialDrugName1, false)!=null);
		assertTrue(engine.isExists("test",diagnosticCode1, commercialDrugName1));
		assertEquals(3,engine.getDataSetSize());
		engine.deleteRecord("test",diagnosticCode1, commercialDrugName1,true);	
	}
	
	@Test
	public void testInsertMultipleDiagnosisWithLessThan1PercentOfDrug1() {

		RiskEngineRepository engine1PercentRisk = new RiskEngineRepository(0.01);

		for (int i=0 ; i<NUMBER_OF_PRESCRIPTIONS; i++){
			engine1PercentRisk.insertRecord("test",diagnosticCode1, commercialDrugName1, false);			
		}		
		
		for (int i=0 ; i<NUMBER_OF_PRESCRIPTIONS; i++){
			engine1PercentRisk.insertRecord("test",diagnosticCode2, commercialDrugName1, false);			
		}	

		String[] diagnosisCodes= {diagnosticCode1,diagnosticCode2,diagnosticCode3};
		engine1PercentRisk.insertRecord("test",diagnosisCodes, commercialDrugName1, false);
		ArrayList<String> expectedValue = new ArrayList<String>();
		assertEquals(expectedValue,engine1PercentRisk.getAnomaliesByCommercialDrugName("test",false));
		assertEquals(NUMBER_OF_PRESCRIPTIONS*2+2,engine1PercentRisk.getDataSetSize());
		
		assertEquals(true,engine1PercentRisk.checkAnomaly("test",diagnosticCode3,commercialDrugName1)!=null);
		
		engine1PercentRisk.clearAllRecords("test");	
		
	}
	
	@Test
	public void testStatsWithLessThan1PercentOfDrug1() {

		RiskEngineRepository engine1PercentRisk = new RiskEngineRepository(0.01);

		for (int i=0 ; i<100; i++){
			engine1PercentRisk.insertRecord("test",diagnosticCode1, commercialDrugName1,false);			
		}		
		
		for (int i=0 ; i<100; i++){
			engine1PercentRisk.insertRecord("test",diagnosticCode1, commercialDrugName2,false);			
		}	
		
		for (int i=0 ; i<100; i++){
			engine1PercentRisk.insertRecord("test",diagnosticCode2, commercialDrugName2,false);			
		}
		
		for (int i=0 ; i<100; i++){
			engine1PercentRisk.insertRecord("test",diagnosticCode2, commercialDrugName2,false);			
		}
		
		for (int i=0 ; i<100; i++){
			engine1PercentRisk.insertRecord("test",diagnosticCode3, commercialDrugName2,false);			
		}

		for (int i=0 ; i<100; i++){
			engine1PercentRisk.insertRecord("test",diagnosticCode3, commercialDrugName1,false);			
		}

		
		engine1PercentRisk.insertRecord("test",diagnosticCode3, commercialDrugName3, false);			


		ArrayList<String> expectedValue = new ArrayList<String>();
		expectedValue.add(engine1PercentRisk.generateKey(diagnosticCode1,engine1PercentRisk.generateKey(commercialDrugName2,"1")));

		ArrayList<Anomalyresult> anomalies;
		anomalies = engine1PercentRisk.getAnomaliesByCommercialDrugName("test",false);

		//		logger.info("expected");
		//		logger.info(expectedValue);
		//		logger.info("anomalies");		
		//		logger.info(anomalies);

		//assertEquals(expectedValue.toString() , anomalies.toString());

		logger.info(engine1PercentRisk.calculateStats("test").toString());
		engine1PercentRisk.clearAllRecords("test");			
	}
	
	
	
	

}
