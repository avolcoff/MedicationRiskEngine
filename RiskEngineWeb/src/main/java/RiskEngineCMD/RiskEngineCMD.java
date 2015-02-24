package RiskEngineCMD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import com.gigmed.riskengine.common.C;
import com.gigmed.riskengine.common.Util;
import com.gigmed.riskengine.dao.Anomalyresult;
import com.gigmed.riskengine.repository.*;

import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVReadProc;

public class RiskEngineCMD {

	static RiskEngineRepository engine;
	private static Logger logger;
	private static Integer diagnosis1ColumnID;
	private static Integer diagnosis2ColumnID;
	private static Integer diagnosis3ColumnID;
	private static Integer drugColumnID;
	
	public static void main(String[] args) {
		logger = Util.getInitializedLogger();
		
		if (args.length<4){
			System.out.println("Invalid command line parameters!\nPlease launch the application in the following format:");
			System.out.println("Usage: java -jar "+Util.APP_NAME+".jar [DataSetInputFileName] [diagnosis column 1 index in input file] [diagnosis column 2 index in input file, 0 if does not exist] [diagnosis column 3 index in input file, 0 if does not exist] [drug column index in input file] [AnomalyOutputFileName] [AnomalyDetectionType - Find anomalies by either 'drug' or 'ingredients'] [Risk Threshold Percentage]");
			System.out.println("Example: java -jar "+Util.APP_NAME+".jar NormalizedDataSet.csv 6 7 8 4 AnomalyOutput.csv drug 0.01");
			return;
		} 
		int argsIndex = 0;

		String inputFilePath = args[argsIndex++];
		diagnosis1ColumnID = Integer.valueOf(args[argsIndex++]);
		diagnosis2ColumnID = Integer.valueOf(args[argsIndex++]);
		diagnosis3ColumnID = Integer.valueOf(args[argsIndex++]);
		drugColumnID = Integer.valueOf(args[argsIndex++]);
		//therapeuticCategoryColumnID  = Integer.valueOf(args[argsIndex++]);
		String outputFilePath = args[argsIndex++];
		String testType = args[argsIndex++];
		double riskThresholdPercent = Double.valueOf(args[argsIndex++]);
		
		initialize(riskThresholdPercent);

		importCSVFileToRiskEngine(inputFilePath, testType);
		checkAnomaliesAndPersistThem(C.DATA_SET_ID_CMD, testType, true, outputFilePath);
	}

	private static void checkAnomaliesAndPersistThem(String dataSetID, String testType, boolean persistToDB, String anomalyOutputFilePath) {
		if (testType.equals(RiskEngineRepository.TEST_BY_DRUG_NAME)){
			long timeBeforeFindingAnomaliesByDrugName = System.currentTimeMillis();
			ArrayList<Anomalyresult> anomalies = engine.getAnomaliesByCommercialDrugName(anomalyOutputFilePath,persistToDB);
			long timeAfterFindingAnomaliesByDrugName = System.currentTimeMillis();
			Util.logProcessingTimeSummary("getAnomaliesByCommercialDrugName",timeBeforeFindingAnomaliesByDrugName, timeAfterFindingAnomaliesByDrugName, logger, engine);
			logger.info("We found these anomalies by Drug Name:"+anomalies.toString());
			System.out.println(engine.calculateStats(anomalyOutputFilePath));
			if (persistToDB)
				Util.exportAnomaliesToDB(C.DATA_SET_ID_CMD, anomalies, engine);
			else
				Util.exportAnomaliesToFile(anomalies,anomalyOutputFilePath+"ByDrug.csv", engine);

			Util.logCalculationSummary(logger, engine);			
		}		


		if (testType.equals(RiskEngineRepository.TEST_BY_INGREDIENT)){

			long timeBeforeFindingAnomaliesByIngredients = System.currentTimeMillis();
			ArrayList<Anomalyresult> anomalies = engine.getAnomaliesByIngredients(dataSetID);
			long timeAfterFindingAnomaliesByIngredients = System.currentTimeMillis();
			Util.logProcessingTimeSummary("getAnomaliesByIngredients",timeBeforeFindingAnomaliesByIngredients, timeAfterFindingAnomaliesByIngredients, logger, engine);
			logger.info("We found these anomalies by Ingredients:"+anomalies.toString());
			if (persistToDB)
				Util.exportAnomaliesToDB(C.DATA_SET_ID_CMD,anomalies, engine);
			else
				Util.exportAnomaliesToFile(anomalies,anomalyOutputFilePath+"ByIngredients.csv", engine);
			Util.logCalculationSummary(logger, engine);
			Set<String> invalidDrugNames = engine.getInvalidDrugNamesArray();
			logger.info("We found "+invalidDrugNames.size()+" invalid Drug Names = "+ invalidDrugNames);
		}	

		sendEmail();

	}

	private static void initialize(double riskThresholdPercent) {
		Util.checkLicense();		
		engine = new RiskEngineRepository(riskThresholdPercent);
	}

	private static void importCSVFileToRiskEngine(final String filePath, final String testType) {
		CSV csv = CSV
				.separator(',')  // delimiter of fields
				.quote('"')      // quote character
				.create();       // new instance is immutable

		long timeBeforeImport = System.currentTimeMillis();

		
		csv.read(filePath, new CSVReadProc() {
			public void procRow(int rowIndex, String... values) {
				logger.info("importing from CSV : "+rowIndex + ": " + Arrays.asList(values));
				String diagnosticCode1 = values[diagnosis1ColumnID-1].trim();
				String diagnosticCode2 = null;
				if (diagnosis2ColumnID!=0)
					diagnosticCode2 = values[diagnosis2ColumnID-1].trim();
				String diagnosticCode3 = null;
				if (diagnosis3ColumnID!=0)
					diagnosticCode3 = values[diagnosis3ColumnID-1].trim();
				//				String therapeuticCategory;
				//				if (therapeuticCategoryColumnID!=0)
				//					therapeuticCategory = values[therapeuticCategoryColumnID-1];

				String drugName = values[drugColumnID-1].trim();

				if (testType.equals(RiskEngineRepository.TEST_BY_DRUG_NAME)){
					if (diagnosis2ColumnID==0 && diagnosis3ColumnID==0)
						engine.insertRecord(filePath,diagnosticCode1, drugName, false, C.DIAGNOSIS_CODE_TYPE_ICD9_GENERALIZED);
					else
					{
						String[] diagnosisCodes = {diagnosticCode1,diagnosticCode2,diagnosticCode3};
						engine.insertRecord(filePath,diagnosisCodes, drugName, false);
					}

				}
				if (testType.equals(RiskEngineRepository.TEST_BY_INGREDIENT))
					engine.insertRecord(filePath, diagnosticCode1, drugName, false, true, C.DIAGNOSIS_CODE_TYPE_ICD9_GENERALIZED);
			}
		});

		long timeAfterImport = System.currentTimeMillis();
		Util.logProcessingTimeSummary("Data Set import",timeBeforeImport, timeAfterImport, logger, engine);
		logger.info("Dataset record count= "+ engine.getDataSetSize());		
	}

	private static void sendEmail() {
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


}
