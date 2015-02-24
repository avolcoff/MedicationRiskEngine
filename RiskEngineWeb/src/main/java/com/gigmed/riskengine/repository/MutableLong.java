package com.gigmed.riskengine.repository;

public class MutableLong {
	/**
	 * 
	 */
	
	MutableLongData data = new MutableLongData(1);
	/**
	 * @param riskEngineRepository
	 */
	MutableLong() {
	}
	
	public MutableLong(long value) {
		data.value = value;
	}
	
	public void increment () { ++data.value;      }
	public long  get ()       { return data.value; }	
}