package com.caseyausmus.reflection

import org.codehaus.groovy.runtime.typehandling.GroovyCastException

import java.beans.IntrospectionException
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

import com.caseyausmus.reflection.testClasses.*
import org.junit.Test

class ReflectionUtilTest {

	@Test
	void testExecuteGetter() throws Exception {
		Parent parent = new Parent(parentString: 'hail hydra')

		String valueByReflection = ReflectionUtil.executeGetter(String, parent, 'parentString')
		assert parent.parentString == valueByReflection
		assert valueByReflection == 'hail hydra'
	}
	
	@Test
	void testExecuteGetterOnChild() throws Exception {
		Parent parent = new Parent()

		//First result should be null since parent.child is null
		String valueByReflection = ReflectionUtil.executeGetter(String, parent, 'child.childString')
		assert valueByReflection == null

		//This result should return a value since the child is initialized now
		parent.child = new Child(childString: 'testChildString')

		valueByReflection = ReflectionUtil.executeGetter(String, parent, 'child.childString')
		assert parent.child.childString == valueByReflection
		assert valueByReflection == 'testChildString'
	}
	
	@Test
	void testExecuteGetterOnGrandchild() throws Exception {
		Parent parent = new Parent()

		//First result should be null since parent.child is null
		Boolean valueByReflection = ReflectionUtil.executeGetter(Boolean, parent, 'child.grandchild.grandchildBoolean');
		assert valueByReflection == null

		//Second result should be null since parent.child.grandchild is null
		parent.child = new Child()

		valueByReflection = ReflectionUtil.executeGetter(Boolean, parent, 'child.grandchild.grandchildBoolean');
		assert valueByReflection == null

		//Third result should be true now that everything is initialized
		parent.child.grandchild = new Grandchild()

		valueByReflection = ReflectionUtil.executeGetter(Boolean, parent, 'child.grandchild.grandchildBoolean');
		assert valueByReflection
	}
	
	@Test
	void testExecuteGetterOnBaseClassField() throws Exception {
		Parent parent = new Parent(baseAttribute: 'baseAttributeValue');

		String valueByReflection = ReflectionUtil.executeGetter(String, parent, "baseAttribute");
		assert parent.baseAttribute == valueByReflection
		assert valueByReflection == 'baseAttributeValue'
	}
	
	@Test
	void testExecuteGetterOnBaseChildClassField() throws Exception {
		Parent parent = new Parent(child: new Child(baseAttribute: 'baseAttributeValue'))

		String valueByReflection = ReflectionUtil.executeGetter(String, parent, 'child.baseAttribute')
		assert parent.child.baseAttribute == valueByReflection
		assert valueByReflection == 'baseAttributeValue'
	}
	
	@Test
	void testExecuteGetterOnBaseGrandchildClassField() throws Exception {
		Parent parent = new Parent(child: new Child(grandchild: new Grandchild(baseAttribute: 'baseAttributeValue')))

		String valueByReflection = ReflectionUtil.executeGetter(String, parent, 'child.grandchild.baseAttribute')
		assert parent.child.grandchild.baseAttribute == valueByReflection
		assert valueByReflection == 'baseAttributeValue'
	}
	
	@Test(expected=MissingPropertyException)
	void testExecuteGetterNoSuchMethod() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Parent parent = new Parent(child: new Child())
		
		//Attempting to call a method that doesn't exist, should throw an exception
		ReflectionUtil.executeGetter(Boolean, parent, 'child.invalidFieldNameGoesHere')
	}
	
	@Test
	void testExcecuteGetterOnNull() throws Exception {
		Parent parent = null
		Date valueByReflection = ReflectionUtil.executeGetter(Date, parent, 'timeIsAFlatCircle')

		assert valueByReflection == null
	}
	
	@Test
	void testExecuteProtectedGetter() throws Exception {
		Parent parent = new Parent()
		String valueByReflection = ReflectionUtil.executeGetter(String, parent, 'protectedMethodValue')

		assert parent.getProtectedMethodValue() == valueByReflection
		assert valueByReflection == Parent.PROTECTED_METHOD_VALUE
	}
	
	@Test
	void testExecutePrivateGetter() throws Exception {
		Parent parent = new Parent();
		String valueByReflection = ReflectionUtil.executeGetter(String, parent, 'privateMethodValue');

		assert parent.getPrivateMethodValue() == valueByReflection
		assert valueByReflection == Parent.PRIVATE_METHOD_VALUE
	}
	
	@Test
	void testExecuteSetter() throws Exception {
		Parent parent = new Parent()
		Child child = new Child()

		ReflectionUtil.executeSetter(parent, 'child', child)
		assert child == parent.child

		Grandchild grandchild = new Grandchild()
		ReflectionUtil.executeSetter(parent, 'child.grandchild', grandchild)
		assert grandchild == parent.child.grandchild

		ReflectionUtil.executeSetter(parent, 'child.grandchild.grandchildBoolean', false)
		assert !parent.child.grandchild.grandchildBoolean
	}
	
	@Test(expected=GroovyCastException)
	void testExecuteSetterWrongType() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
		Parent parent = new Parent()
		
		//Attempting to set a String value on a Date field, should throw exception
		ReflectionUtil.executeSetter(parent, 'timeIsAFlatCircle', 'testString');
	}
	
	@Test(expected=MissingPropertyException)
	void testExecuteSetterNoSuchMethod() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
		Parent parent = new Parent()
		
		//Attempting to set a value on a field that doesn't exist, should throw exception
		ReflectionUtil.executeSetter(parent, 'blargle', 'testValue');
	}
	
	@Test
	void testGetFieldsForAnnotation() {
		//The parent class has one field annotated with @Column
		List<Field> results = ReflectionUtil.getFieldsForAnnotation(Parent, TestAnnotation)
		assert results.size() == 1
		assert "id" == results[0].name
		
		//No fields are annotated with @Test, so this should return an empty list
		results = ReflectionUtil.getFieldsForAnnotation(Parent, Test)
		assert results.size() == 0
	}
	
	@Test
	void testDoesFieldExist() {
		//Testing fields that exist on the target class
		boolean fieldExists = ReflectionUtil.doesFieldExist(Parent, 'parentString')
		assert fieldExists
		
		fieldExists = ReflectionUtil.doesFieldExist(Parent, 'child.childString')
		assert fieldExists
		
		fieldExists = ReflectionUtil.doesFieldExist(Parent, 'child.grandchild.grandchildBoolean')
		assert fieldExists
		
		//Testing fields that do not exist on the target class
		fieldExists = ReflectionUtil.doesFieldExist(Parent, 'invalidFieldName')
		assert !fieldExists
		
		fieldExists = ReflectionUtil.doesFieldExist(Parent, 'child.invalidFieldName')
		assert !fieldExists
		
		fieldExists = ReflectionUtil.doesFieldExist(Parent, 'child.grandchild.invalidFieldName')
		assert !fieldExists
	}

	@Test
	void testDoesFieldExistForString() {
		//Testing fields that exist on the target class
		boolean fieldExists = ReflectionUtil.doesFieldExist('com.caseyausmus.reflection.testClasses.Parent', 'parentString')
		assert fieldExists

		//Testing fields that do not exist on the target class
		fieldExists = ReflectionUtil.doesFieldExist('com.caseyausmus.reflection.testClasses.Parent', 'invalidFieldName')
		assert !fieldExists
	}
	
	@Test
	void testGetField() throws Exception {
		//Get a field off the Parent class
		Field field = ReflectionUtil.getField(Parent, 'parentString');
		assert 'parentString' == field.name
		assert String == field.type

		//Get another field from Parent
		field = ReflectionUtil.getField(Parent, 'timeIsAFlatCircle')
		assert 'timeIsAFlatCircle' == field.name
		assert Date == field.type

		//Get a field off of BaseParent, which Parent extends
		field = ReflectionUtil.getField(Parent, 'baseAttribute');
		assert 'baseAttribute' == field.name
		assert String == field.type
	}

	@Test
	void testGetFieldWithString() throws Exception {
		//Test string-based method
		Field field = ReflectionUtil.getField('com.caseyausmus.reflection.testClasses.Parent', 'parentString');
		assert 'parentString' == field.name
		assert String == field.type
	}
	
	@Test
	void testGetFieldOnChild() throws Exception {
		//Get the Child field from Parent
		Field field = ReflectionUtil.getField(Parent, 'child')
		assert 'child' == field.name
		assert Child == field.type

		//Get an attribute from the Child class
		field = ReflectionUtil.getField(Parent, 'child.childString')
		assert 'childString' == field.name
		assert String == field.type

		//Get a field off of BaseChild, which child extends
		field = ReflectionUtil.getField(Parent, 'child.baseAttribute');
		assert 'baseAttribute' == field.name
		assert String == field.type
	}
	
	@Test
	void testGetFieldOnGrandChild() throws Exception {
		//Get the Child field from Parent
		Field field = ReflectionUtil.getField(Parent, 'child.grandchild')
		assert 'grandchild' == field.name
		assert Grandchild == field.type

		//Get an attribute from the Child class
		field = ReflectionUtil.getField(Parent, 'child.grandchild.grandchildBoolean')
		assert 'grandchildBoolean' == field.name
		assert boolean.class == field.type

		//Get a field off of BaseChild, which child extends
		field = ReflectionUtil.getField(Parent, 'child.grandchild.baseAttribute')
		assert 'baseAttribute' == field.name
		assert String == field.type
	}

	@Test(expected = NoSuchFieldException)
	void testGetInvalidField() {
		//Testing getting fields that don' exist
		ReflectionUtil.getField(Parent, 'invalidField')
		ReflectionUtil.getField(Parent, 'child.invalidField')
		ReflectionUtil.getField(Parent, 'child.grandchild.invalidField')
	}
	
	@Test
	void testGetFieldsWithStatics() {
		List<Field> fields = ReflectionUtil.getFields(ObjectWithStatics)
		assert 3 == fields.size()
	}
	
	@Test
	void testGetFieldsWithoutStatics() {
		List<Field> fields = ReflectionUtil.getNonStaticFields(ObjectWithStatics)
		assert 2 == fields.size()
	}
	
	@Test
	void testGetDeclaredMethod() throws NoSuchMethodException {
		Method publicMethod = ReflectionUtil.getDeclaredMethod(Parent, 'getParentString')
		Method protectedMethod = ReflectionUtil.getDeclaredMethod(Parent, 'getProtectedMethodValue')
		Method privateMethod = ReflectionUtil.getDeclaredMethod(Parent, 'getPrivateMethodValue')
		Method superMethod = ReflectionUtil.getDeclaredMethod(Parent, 'getBaseAttribute')
		
		assert publicMethod
		assert protectedMethod
		assert privateMethod
		assert superMethod
	}

	@Test(expected = NoSuchMethodException)
	void testGetInvalidDeclaredMethod() {
		ReflectionUtil.getDeclaredMethod(Parent, 'invalidMethod')
	}

	@Test
	void testGetFieldsOfType() {
		List<Field> fields = ReflectionUtil.getFieldsOfType(Parent, Child)
		assert fields.size() == 1

		fields = ReflectionUtil.getFieldsOfType(Parent, String)
		assert fields.size() == 4 // expecting 2 static strings, 2 non-static
	}

	@Test
	void testGetFieldsOfTypeWithString() {
		//Test string-based method
		List<Field> fields = ReflectionUtil.getFieldsOfType('com.caseyausmus.reflection.testClasses.Parent', 'java.lang.String')
		assert fields.size() == 4
	}

	@Test
	void testGetFields() {
		assert ReflectionUtil.getFields(Parent).size() == 7
		assert ReflectionUtil.getFields(Child).size() == 3
		assert ReflectionUtil.getFields(Grandchild).size() == 2
	}
}
