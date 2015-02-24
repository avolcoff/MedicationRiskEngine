package com.gigmed.riskengine.common;

public class C {
	public static final char SECOND_BEST_DELIMITER = '-';
	public static final String URL_SEARCH_ICD_9 = "http://www.mdhealthresource.com/icd-9-cm-medical-code/";
	public static final String URL_SEARCH_DRUG_NAME = "http://www.drugs.com/search.php?searchterm=";

	private static final String WORKING_FILE_PATH = "";
	public static final String CACHE_FOLDER_FILE_PATH = WORKING_FILE_PATH+"CACHE/";

	public static final String XPATH_RXNORM_ID_TEXT = "//rxnormId/text()";
	public static final String XPATH_APPROXIMATE_RXNORM_ID_TEXT = "//rxnormdata/approximateGroup/candidate/rxcui/text()";
	public static final String URL_RXNORM_CONVERT_RXCUI_TO_INGREDIENTS_PART_1 = "http://rxnav.nlm.nih.gov/REST/rxcui/";
	public static final String URL_RXNORM_CONVERT_RXCUI_TO_INGREDIENTS_PART_2 = "/related?tty=IN+PIN";
	public static final String URL_RXNORM_CONVERT_DRUG_NAME_TO_RXCUI = "http://rxnav.nlm.nih.gov/REST/rxcui?name=";
	public static final String URL_APPROXIMATE_RXNORM_CONVERT_DRUG_NAME_TO_RXCUI = "http://rxnav.nlm.nih.gov/REST/approximateTerm?maxEntries=1&term=";
	public static final String KEY_DELIMITER = ":";
	public static final int DIAGNOSIS_CODE_TYPE_ICD9_GENERALIZED = 0;
	public static final long INSTANCE_FREQUENCY_COUNT_THRESHOLD = 1;
	public static final char CSV_DELIMITER = ',';
	public static final String CSV_DELIMITER_STRING = String.valueOf(CSV_DELIMITER);
	public static final String THIRD_BEST_DELIMITER = "#";
	public static final String DB_TABLE_INPUT_DATASET = "inputdataset";
	public static final String DATA_SET_ID_CMD = "cmdFile";
	public static final int COMMIT_BATCH_SIZE = 20;
}
