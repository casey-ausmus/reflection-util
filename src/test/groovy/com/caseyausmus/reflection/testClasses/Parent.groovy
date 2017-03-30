package com.caseyausmus.reflection.testClasses

class Parent extends BaseParent {
	public static final String PROTECTED_METHOD_VALUE = 'protected value'
	public static final String PRIVATE_METHOD_VALUE = 'private value'

	@TestAnnotation
	Integer id

	String parentString
	Child child
	Date timeIsAFlatCircle
	
	protected static String getProtectedMethodValue() {
		return PROTECTED_METHOD_VALUE;
	}

	private static String getPrivateMethodValue() {
		return PRIVATE_METHOD_VALUE;
	}
}
