package com.gigmed.riskengine.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.hibernate.SessionFactory;

import com.gigmed.riskengine.dao.Anomalyresult;
import com.gigmed.riskengine.repository.RiskEngineRepository;

public class Util {

	private static final String LICENSE_EXPIRY_DATE = "2015-02-15";
	public static final String APP_NAME = "GigMed";
	static Properties mailServerProperties;
	static Session getMailSession;
	static MimeMessage generateMailMessage;

	public static void sendEmail(String to, String subject, String emailBody) throws AddressException, MessagingException {

		//Step1		
		System.out.println("\n 1st ===> setup Mail Server Properties..");
		mailServerProperties = System.getProperties();
		mailServerProperties.put("mail.smtp.port", "587");
		mailServerProperties.put("mail.smtp.auth", "true");
		mailServerProperties.put("mail.smtp.starttls.enable", "true");
		System.out.println("Mail Server Properties have been setup successfully..");

		//Step2		
		System.out.println("\n\n 2nd ===> get Mail Session..");
		getMailSession = Session.getDefaultInstance(mailServerProperties, null);
		generateMailMessage = new MimeMessage(getMailSession);
		generateMailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		generateMailMessage.setSubject(subject);
		generateMailMessage.setContent(emailBody, "text/html");
		System.out.println("Mail Session has been created successfully..");

		//Step3		
		System.out.println("\n\n 3rd ===> Get Session and Send mail");
		Transport transport = getMailSession.getTransport("smtp");

		// Enter your correct gmail UserID and Password (XXXarpitshah@gmail.com)
		transport.connect("smtp.gmail.com", "medsalertnotification", "screen15g");
		transport.sendMessage(generateMailMessage, generateMailMessage.getAllRecipients());
		transport.close();
	}

	public static void saveCacheToFile(String filePath, Object object){
		try {
			FileOutputStream fout = new FileOutputStream(filePath);
			ObjectOutputStream oos;

			oos = new ObjectOutputStream(fout);

			oos.writeObject(object);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	public static Object loadCacheFromFile(String filePath) {
		Map cacheObject = new HashMap();
		try {
			FileInputStream fin = new FileInputStream(filePath);
			ObjectInputStream ois = new ObjectInputStream(fin);
			cacheObject =  (Map) ois.readObject();
			if (cacheObject==null)
				cacheObject=new HashMap();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return cacheObject;
	}

	public static void logProcessingTimeSummary(String processName, long timeBeforeImport,
			long timeAfterImport, Logger logger, RiskEngineRepository engine) {
		logger.info("Total "+processName+" processing time(in seconds)="+((double)(timeAfterImport-timeBeforeImport))/1000);
		logger.info(processName+" processing time per record(in seconds)="+((double)(timeAfterImport-timeBeforeImport))/1000/engine.getDataSetSize());
	}

	public static Logger getInitializedLogger() {
		FileHandler fh;  
		Logger logger = Logger.getLogger("MyLog");

		try {  

			// This block configure the logger with handler and formatter
			new File("log").mkdir();
			fh = new FileHandler("log/"+APP_NAME+".log");  
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();  
			fh.setFormatter(formatter);          

		} catch (SecurityException e) {  
			e.printStackTrace();  
		} catch (IOException e) {  
			e.printStackTrace();  
		}			

		return logger;
	}

	public static void exportAnomaliesToFile(ArrayList<Anomalyresult> anomalies, String fileName, RiskEngineRepository engine) {	
		int counter = 0;
		File file = new File(fileName.substring(0, fileName.lastIndexOf("\\")));
		file.mkdirs();
		try
		{
			FileWriter writer = new FileWriter(fileName);

			writer.append("RISK SCORE");			
			writer.append(',');	

			writer.append("KEY COMBINATION");
			writer.append(',');			
			writer.append("DIAGNOSIS-DRUG COMBINATION FREQUENCY IN DATASET");
			writer.append(',');	 

			writer.append("DIAGNOSIS 1 CODE (ICD-9)");
			writer.append(',');
			writer.append("DIAGNOSIS 1 FREQUENCY IN DATASET");
			writer.append(',');

			writer.append("DIAGNOSIS 2 CODE (ICD-9)");
			writer.append(',');
			writer.append("DIAGNOSIS 2 FREQUENCY IN DATASET");
			writer.append(',');

			writer.append("DIAGNOSIS 3 CODE (ICD-9)");
			writer.append(',');
			writer.append("DIAGNOSIS 3 FREQUENCY IN DATASET");
			writer.append(',');

			writer.append("DRUG NAME");
			writer.append(',');	 			 
			writer.append("DRUG/INGREDIENT FREQUENCY IN DATASET");
			writer.append(',');		

			writer.append("ANOMALY REASON");
			writer.append(',');

			writer.append("DIAGNOSIS CODE (ICD-9) DESCRIPTION");
			writer.append(',');	
			writer.append("DRUG NAME DESCRIPTION");				
			writer.append(',');


			writer.append('\n');


			for (Anomalyresult result: anomalies){

				writer.append(Double.toString(result.getRiskScore()));				
				writer.append(C.CSV_DELIMITER);	

				writer.append(result.getKeyCombination());
				writer.append(C.CSV_DELIMITER);	
				writer.append(Long.toString(result.getKeyFrequency()));
				writer.append(C.CSV_DELIMITER);	

				writer.append(result.getDiagnosis1());
				writer.append(C.CSV_DELIMITER);	
				writer.append(Long.toString(result.getDiagnosis1frequency()));
				writer.append(C.CSV_DELIMITER);	

				writer.append(result.getDiagnosis2());
				writer.append(C.CSV_DELIMITER);	
				writer.append(Long.toString(result.getDiagnosis2frequency()));
				writer.append(C.CSV_DELIMITER);

				writer.append(result.getDiagnosis3());
				writer.append(C.CSV_DELIMITER);	
				writer.append(Long.toString(result.getDiagnosis3frequency()));
				writer.append(C.CSV_DELIMITER);	

				writer.append(result.getDrugName());
				writer.append(C.CSV_DELIMITER);	
				writer.append(Long.toString(result.getDrugFrequency()));
				writer.append(C.CSV_DELIMITER);	

				writer.append(result.getReason());

				try {
					writer.append(C.CSV_DELIMITER);
					// add icd-9 description
					String searchResult = engine.getICD9Description(result.getKeyCombination().split(C.KEY_DELIMITER)[0]);
					if (searchResult!=null)
						writer.append(searchResult.replace(C.CSV_DELIMITER, C.SECOND_BEST_DELIMITER));

					writer.append(C.CSV_DELIMITER);
					// add drug name description
					String searchResult2 = engine.getDrugDescription(result.getKeyCombination().split(C.KEY_DELIMITER)[1]);
					if (searchResult2!=null)
						writer.append(searchResult2.replace(C.CSV_DELIMITER, C.SECOND_BEST_DELIMITER));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	

				writer.append('\n');

				System.out.println(counter++);
				//writer.flush();
			}

			writer.flush();
			writer.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		} 

	}

	public static void logCalculationSummary(Logger logger, RiskEngineRepository engine) {
		logger.info("Anomaly Count = "+ engine.getLastCalculatedAnomalySize());
		logger.info("Dataset record count= "+ engine.getDataSetSize());
		logger.info("Percentage of Anomalies out of total dataset="+engine.getAnomalyPercentage()+"%");
	}

	public static void checkLicense() {
		String myTextDate = LICENSE_EXPIRY_DATE;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date myDate = null;
		try {
			myDate = sdf.parse(myTextDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (new GregorianCalendar().getTime().after(myDate)){
			System.out.println("your License has expired on "+LICENSE_EXPIRY_DATE);
			System.out.println("Please contact Ari to get a new license at ari@volcoff.com");
			System.exit(-1);
		}
	}

	public static void exportAnomaliesToDB(String dataSetID, ArrayList<Anomalyresult> anomalies, RiskEngineRepository engine) {
		SessionFactory sessionFactory = HibernateUtil.getSessionFactory();  
		org.hibernate.Session session = sessionFactory.openSession();  
		session.beginTransaction();  

		String hqlDelete = "delete Anomalyresult where datasetId = :dataSetID";
		int deletedEntities = session.createQuery( hqlDelete )
				.setString( "dataSetID", dataSetID )
				.executeUpdate();
						
		for (Anomalyresult result:anomalies){
			// System.out.println(result);
			
			result.setDatasetId(dataSetID);
						
			result.setIcd9description(engine.getICD9Description(result.getKeyCombination().split(C.KEY_DELIMITER)[0]));
					
			result.setDrugNameDescription(engine.getDrugDescription(result.getKeyCombination().split(C.KEY_DELIMITER)[1]));
								
			session.persist(result);
		}

		
		session.getTransaction().commit();
		
		session.close();  
	}
}
