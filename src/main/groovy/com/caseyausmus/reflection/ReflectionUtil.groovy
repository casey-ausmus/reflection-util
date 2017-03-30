package com.caseyausmus.reflection

import java.beans.IntrospectionException
import java.lang.annotation.Annotation
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class ReflectionUtil {

	/**
	 * Retrieve all fields of the specified targetClass
	 *
	 * @param className Name of class to reflect over
	 * @param targetClass Name of class to retrieve fields for
	 * @return All fields of the given type
	 */
	static List<Field> getFieldsOfType(String className, String targetClass) throws ClassNotFoundException {
		return getFieldsOfType(Class.forName(className), Class.forName(targetClass))
	}

	/**
	 * Retrieve all fields of the specified targetClass
	 *
	 * @param clazz Class to reflect over
	 * @param targetClass Class to retrieve fields for
	 * @return All fields of the given type
	 */
	static List<Field> getFieldsOfType(Class<?> clazz, Class<?> targetClass) {
        return getFields(clazz).findAll { it.type == targetClass }
	}

	/**
	 * Retrieve a specific field from a class
	 * 
	 * @param className Name of the class to reflect over
	 * @param fieldName Name of the field to retrieve
	 * @return The field from the given class with the given name
	 * @throws ClassNotFoundException
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	static Field getField(String className, String fieldName) throws ClassNotFoundException, NoSuchFieldException, SecurityException {
		return getField(Class.forName(className), fieldName)
	}
	
	/**
	 * Retrieve a specific field from a class.  The field name can be period delimited (e.g. "field1.field2") and this
	 * method will traverse each field and return the final field.
	 * 
	 * @param clazz Class to reflect over
	 * @param fieldName Name of the field to retrieve
	 * @return The field from the given class with the given name
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
		Field field = null
		Class<?> lastClass = clazz

		String[] fieldTokens = fieldName.tokenize('.')

		for(String fieldToken : fieldTokens) {
			try {
				field = lastClass.getDeclaredField(fieldToken)
			}
			catch(NoSuchFieldException e) {
				if(clazz.superclass) {
					field = getField(lastClass.superclass, fieldToken)
				}
				else {
					throw e
				}
			}

			lastClass = field.type
		}
		
		return field
	}
	
	/**
	 * Checks if a given field exists on a class
	 * 
	 * @param className Name of the class to reflect over
	 * @param fieldName Name of the field to check
	 * @return True if the field exists, false otherwise
	 * @throws ClassNotFoundException
	 */
	static boolean doesFieldExist(String className, String fieldName) throws ClassNotFoundException {
		return doesFieldExist(Class.forName(className), fieldName)
	}
	
	/**
	 * Checks if a given field exists on a class
	 * 
	 * @param clazz Class to reflect over
	 * @param fieldName Name of the field to check
	 * @return True if the field exists, false otherwise
	 */
	static boolean doesFieldExist(Class<?> clazz, String fieldName) {
		try {
			getField(clazz, fieldName)
		} catch (SecurityException | NoSuchFieldException e) {
			return false
		}
		
		return true
	}
	
	/**
	 * Executes getter methods on a target object.  This method will string together getter calls if the fieldName value
	 * passed in is period delimited.  For example, a field name of "one.two" will result in a call to getOne(), then the
	 * object returned by getOne() will have getTwo() executed on it.
	 * 
	 * @param <T>
	 * @param returnType Type of the object you expect to be returned
	 * @param target Object to reflect over
	 * @param fieldName Name of the field to execute the getter for
	 * @return The value of the final getter called on the target object
	 * @throws IntrospectionException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 */
	static <T> T executeGetter(Class<T> returnType, Object target, String fieldName) {
        String[] fieldTokens = fieldName.tokenize('.')
        def lastTarget = target

        fieldTokens.each { field -> lastTarget = lastTarget?."${field}" }

        return lastTarget as T
	}
	
	/**
	 * Executes a setter method on a target object.  This method will string together getter calls if the fieldName value
	 * passed in is period delimited.  For example, a field name of "one.two" will result in a call to getOne(), then the
	 * object returned by getOne() will have setTwo() executed on it.
	 * 
	 * @param target Object to reflect over
	 * @param fieldName Name of the field to execute the setter for
	 * @param value The value to pass into the setter method
	 * @throws IntrospectionException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException 
	 */
	static void executeSetter(Object target, String fieldName, Object value) {
        def lastTarget = target
        String lastField = fieldName
        int idx = fieldName.lastIndexOf('.')

        if(idx > 0) {
            lastTarget = executeGetter(Object, target, fieldName.substring(0, idx))
            lastField = fieldName.substring(idx + 1)
        }

        lastTarget."${lastField}" = value
	}
	
	/**
	 * Retrieve all fields annotated with the given annotation class.
	 * 
	 * @param clazz Class to reflect over
	 * @param annotationClass Annotation class to search for
	 * @return All fields that are annotated with the given annotation class
	 */
	static List<Field> getFieldsForAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        return getFields(clazz, [], false).findAll { it.isAnnotationPresent(annotationClass) }
	}
	
	static Method getDeclaredMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
		try {
			return clazz.getDeclaredMethod(methodName)
		} 
		catch (NoSuchMethodException e) {
			if(clazz.superclass != Object) {
				return getDeclaredMethod(clazz.superclass, methodName)
			}
			else {
				throw e
			}
		}
	}
	
	/**
	 * Retrieve all fields from a class
	 * 
	 * @param clazz Class to reflect over
	 * @return All fields in a class
	 */
	static List<Field> getFields(Class<?> clazz) {
		return getFields(clazz, []);
	}
	
	/**
	 * Retrieve all non-static fields from a class
	 * 
	 * @param clazz Class to reflect over
	 * @return All non-static fields in a class
	 */
	static List<Field> getFieldsWithoutStatics(Class<?> clazz) {
        return getFields(clazz).findAll { !Modifier.isStatic(it.modifiers) }
	}
	
	/**
	 * Retrieve all fields from a class
	 * 
	 * @param clazz Class to reflect over
	 * @param fields List of Field objects
	 * @return All fields in a class
	 */
	static List<Field> getFields(Class<?> clazz, List<Field> fields) {
		return getFields(clazz, fields, true)
	}

	static List<Field> getFields(Class<?> clazz, List<Field> fields, boolean includeBaseClasses) {
		fields.addAll(clazz.declaredFields.toList())

		if(!includeBaseClasses) {
			return fields
		}
		else if (clazz.superclass) {
			return getFields(clazz.superclass, fields, true);
		}
		else {
			return fields
		}
	}
}
