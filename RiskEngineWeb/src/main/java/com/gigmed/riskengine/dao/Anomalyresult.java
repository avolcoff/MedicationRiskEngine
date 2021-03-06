package com.gigmed.riskengine.dao;

// Generated Feb 3, 2015 3:08:05 PM by Hibernate Tools 4.0.0

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import static javax.persistence.GenerationType.IDENTITY;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Anomalyresult generated by hbm2java
 */
@Entity
@Table(name = "anomalyresult", catalog = "gigmed")
public class Anomalyresult implements java.io.Serializable {

	private Integer id;
	private String datasetId;
	private Double riskScore;
	private String keyCombination;
	private String diagnosis1;
	private String diagnosis2;
	private String diagnosis3;
	private String drugName;
	private Long keyFrequency;
	private Long diagnosis1frequency;
	private Long diagnosis2frequency;
	private Long diagnosis3frequency;
	private Long drugFrequency;
	private String reason;
	private String icd9description;
	private String drugNameDescription;
	private String recommendedDrugAlternative;

	public Anomalyresult() {
	}

	public Anomalyresult(String datasetId, Double riskScore,
			String keyCombination, String diagnosis1, String diagnosis2,
			String diagnosis3, String drugName, Long keyFrequency,
			Long diagnosis1frequency, Long diagnosis2frequency,
			Long diagnosis3frequency, Long drugFrequency, String reason,
			String icd9description, String drugNameDescription,
			String recommendedDrugAlternative) {
		this.datasetId = datasetId;
		this.riskScore = riskScore;
		this.keyCombination = keyCombination;
		this.diagnosis1 = diagnosis1;
		this.diagnosis2 = diagnosis2;
		this.diagnosis3 = diagnosis3;
		this.drugName = drugName;
		this.keyFrequency = keyFrequency;
		this.diagnosis1frequency = diagnosis1frequency;
		this.diagnosis2frequency = diagnosis2frequency;
		this.diagnosis3frequency = diagnosis3frequency;
		this.drugFrequency = drugFrequency;
		this.reason = reason;
		this.icd9description = icd9description;
		this.drugNameDescription = drugNameDescription;
		this.recommendedDrugAlternative = recommendedDrugAlternative;
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "ID", unique = true, nullable = false)
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "datasetId", length = 65535)
	public String getDatasetId() {
		return this.datasetId;
	}

	public void setDatasetId(String datasetId) {
		this.datasetId = datasetId;
	}

	@Column(name = "riskScore", precision = 22, scale = 0)
	public Double getRiskScore() {
		return this.riskScore;
	}

	public void setRiskScore(Double riskScore) {
		this.riskScore = riskScore;
	}

	@Column(name = "keyCombination", length = 65535)
	public String getKeyCombination() {
		return this.keyCombination;
	}

	public void setKeyCombination(String keyCombination) {
		this.keyCombination = keyCombination;
	}

	@Column(name = "diagnosis1", length = 65535)
	public String getDiagnosis1() {
		return this.diagnosis1;
	}

	public void setDiagnosis1(String diagnosis1) {
		this.diagnosis1 = diagnosis1;
	}

	@Column(name = "diagnosis2", length = 65535)
	public String getDiagnosis2() {
		return this.diagnosis2;
	}

	public void setDiagnosis2(String diagnosis2) {
		this.diagnosis2 = diagnosis2;
	}

	@Column(name = "diagnosis3", length = 65535)
	public String getDiagnosis3() {
		return this.diagnosis3;
	}

	public void setDiagnosis3(String diagnosis3) {
		this.diagnosis3 = diagnosis3;
	}

	@Column(name = "drugName", length = 65535)
	public String getDrugName() {
		return this.drugName;
	}

	public void setDrugName(String drugName) {
		this.drugName = drugName;
	}

	@Column(name = "keyFrequency")
	public Long getKeyFrequency() {
		return this.keyFrequency;
	}

	public void setKeyFrequency(Long keyFrequency) {
		this.keyFrequency = keyFrequency;
	}

	@Column(name = "diagnosis1Frequency")
	public Long getDiagnosis1frequency() {
		return this.diagnosis1frequency;
	}

	public void setDiagnosis1frequency(Long diagnosis1frequency) {
		this.diagnosis1frequency = diagnosis1frequency;
	}

	@Column(name = "diagnosis2Frequency")
	public Long getDiagnosis2frequency() {
		return this.diagnosis2frequency;
	}

	public void setDiagnosis2frequency(Long diagnosis2frequency) {
		this.diagnosis2frequency = diagnosis2frequency;
	}

	@Column(name = "diagnosis3Frequency")
	public Long getDiagnosis3frequency() {
		return this.diagnosis3frequency;
	}

	public void setDiagnosis3frequency(Long diagnosis3frequency) {
		this.diagnosis3frequency = diagnosis3frequency;
	}

	@Column(name = "drugFrequency")
	public Long getDrugFrequency() {
		return this.drugFrequency;
	}

	public void setDrugFrequency(Long drugFrequency) {
		this.drugFrequency = drugFrequency;
	}

	@Column(name = "reason", length = 65535)
	public String getReason() {
		return this.reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@Column(name = "icd9Description", length = 65535)
	public String getIcd9description() {
		return this.icd9description;
	}

	public void setIcd9description(String icd9description) {
		this.icd9description = icd9description;
	}

	@Column(name = "drugNameDescription", length = 65535)
	public String getDrugNameDescription() {
		return this.drugNameDescription;
	}

	public void setDrugNameDescription(String drugNameDescription) {
		this.drugNameDescription = drugNameDescription;
	}

	@Column(name = "recommendedDrugAlternative", length = 65535)
	public String getRecommendedDrugAlternative() {
		return this.recommendedDrugAlternative;
	}

	public void setRecommendedDrugAlternative(String recommendedDrugAlternative) {
		this.recommendedDrugAlternative = recommendedDrugAlternative;
	}

}
