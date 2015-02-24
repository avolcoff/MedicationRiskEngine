package com.gigmed.riskengine.algorithm;

import java.util.Map;
import java.util.Set;

import com.gigmed.riskengine.common.C;
import com.gigmed.riskengine.dao.Anomalyresult;
import com.gigmed.riskengine.repository.MutableLong;
import com.gigmed.riskengine.repository.StringUtil;

public class RiskEngineAlgorithms {
	public static Anomalyresult checkAnomalyA(String datasetId, String keyCombination, Map<String, MutableLong> hashFrequency, Map<String, MutableLong> fieldFreq, double m_riskPercentageThreshold) {
		MutableLong keyFreqObject = hashFrequency.get(keyCombination);
		long keyFrequency;
		if (keyFreqObject==null)
			keyFrequency = 0;
		else		
			keyFrequency = keyFreqObject.get();

		double riskValue = 1;
		String delimitedFieldNamesAndFrequency = StringUtil.EMPTY_STRING;

		String reason = "";
		String delim = "";
		for (String fieldName: keyCombination.split(C.KEY_DELIMITER)) {
			if (StringUtil.isEmpty(fieldName))
				continue;
			MutableLong fieldFreqObject = fieldFreq.get(fieldName);
			long fieldFrequency;
			if (fieldFreqObject == null)
				fieldFrequency = 0;
			else 
				fieldFrequency = fieldFreqObject.get();

			if (fieldFrequency!=0)
				riskValue *= (double)keyFrequency/fieldFrequency;			

			delimitedFieldNamesAndFrequency +=delim+fieldName+C.KEY_DELIMITER+fieldFrequency;
			String[] fields = keyCombination.split(C.KEY_DELIMITER);

			reason += "*(Instance frequency of "+fields[0]+C.THIRD_BEST_DELIMITER;
			if (fields.length>1)
				reason+=keyCombination.split(C.KEY_DELIMITER)[1].replace("/", C.THIRD_BEST_DELIMITER);
			reason+="/Field frequency of "+fieldName+")"; 

			delim = C.KEY_DELIMITER;
		}

		if (riskValue<m_riskPercentageThreshold){			
			reason = reason.substring(1)+"< Risk Threshold of "+m_riskPercentageThreshold*100+"%";			
			double riskScore = 1/riskValue;
			return new Anomalyresult(datasetId,
					Double.valueOf(riskScore),
					keyCombination,
					delimitedFieldNamesAndFrequency.split(C.KEY_DELIMITER)[0],
					"",
					"",
					delimitedFieldNamesAndFrequency.split(C.KEY_DELIMITER)[2],
					keyFrequency,
					Long.valueOf(delimitedFieldNamesAndFrequency.split(C.KEY_DELIMITER)[1]),
					(long)0,
					(long)0,
					Long.valueOf(delimitedFieldNamesAndFrequency.split(C.KEY_DELIMITER)[3]),
					reason,
					"",
					"",
					getRecommendedDrugs(keyCombination.split(C.KEY_DELIMITER)[0],hashFrequency));
					//riskScore,keyCombination, keyFrequency,delimitedFieldNamesAndFrequency.split(C.KEY_DELIMITER)[0],"","",keyFrequency,delimitedFieldNamesAndFrequency.split(C.KEY_DELIMITER)[2],keyFrequency,delimitedFieldNamesAndFrequency.split(C.KEY_DELIMITER)[1],keyFrequency,delimitedFieldNamesAndFrequency.split(C.KEY_DELIMITER)[3], reason);
		}
		return null;
	}

	private static String getRecommendedDrugs(String diagnosisID, Map<String, MutableLong> hashFrequency) {
		Set<String> set = hashFrequency.keySet();
		long maxValue = 0;
		String recommendedDrug = null;
		for (String hashKey: set){
			long tempValue = hashFrequency.get(hashKey).get();
			if (hashKey.contains(diagnosisID) && tempValue>maxValue){
				maxValue=tempValue;
				recommendedDrug=hashKey.split(C.KEY_DELIMITER)[1];
			}
		}
		return recommendedDrug;
		
	}
}
