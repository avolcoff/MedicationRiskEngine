package com.gigmed.riskengine.repository;

public class StringUtil {

	public static final String EMPTY_STRING = "";
	
	public static boolean notEmpty(String s) {
		return !isEmpty(s);
	}

	public static boolean isEmpty(String s) {
		return (s==null || s.equals(EMPTY_STRING));
	}

}
