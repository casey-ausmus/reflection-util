package com.caseyausmus.reflection.testClasses;

public class Child extends BaseChild {
	private String childString;
	private Grandchild grandchild;

	public String getChildString() {
		return childString;
	}

	public void setChildString(String childString) {
		this.childString = childString;
	}

	public Grandchild getGrandchild() {
		return grandchild;
	}

	public void setGrandchild(Grandchild grandChild) {
		this.grandchild = grandChild;
	}
}
