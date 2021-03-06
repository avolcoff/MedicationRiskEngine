package com.gigmed.riskengine.dao;

// Generated Feb 3, 2015 3:08:05 PM by Hibernate Tools 4.0.0

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Diagnosticvsdrugfrequency generated by hbm2java
 */
@Entity
@Table(name = "diagnosticvsdrugfrequency", catalog = "gigmed")
public class Diagnosticvsdrugfrequency implements java.io.Serializable {

	private String hashkey;
	private Long value;
	private String datasetId;

	public Diagnosticvsdrugfrequency() {
	}

	public Diagnosticvsdrugfrequency(String hashkey) {
		this.hashkey = hashkey;
	}

	public Diagnosticvsdrugfrequency(String hashkey, Long value,
			String datasetId) {
		this.hashkey = hashkey;
		this.value = value;
		this.datasetId = datasetId;
	}

	@Id
	@Column(name = "hashkey", unique = true, nullable = false, length = 500)
	public String getHashkey() {
		return this.hashkey;
	}

	public void setHashkey(String hashkey) {
		this.hashkey = hashkey;
	}

	@Column(name = "value")
	public Long getValue() {
		return this.value;
	}

	public void setValue(Long value) {
		this.value = value;
	}

	@Column(name = "datasetId", length = 65535)
	public String getDatasetId() {
		return this.datasetId;
	}

	public void setDatasetId(String datasetId) {
		this.datasetId = datasetId;
	}

}
