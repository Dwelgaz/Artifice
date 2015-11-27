package com.artifice.refactoring.engine;

import java.util.LinkedList;

import com.artifice.refactoring.data.FieldJob;
import com.artifice.refactoring.data.MethodJob;
import com.artifice.refactoring.data.VariableJob;

public class RefactoringLists {

	private LinkedList<MethodJob> methods;
	private LinkedList<FieldJob> fields;
	private LinkedList<VariableJob> variables;
	
	public LinkedList<MethodJob> getMethods() {
		return methods;
	}

	public void setMethods(LinkedList<MethodJob> methods) {
		this.methods = methods;
	}

	public LinkedList<FieldJob> getFields() {
		return fields;
	}

	public void setFields(LinkedList<FieldJob> fields) {
		this.fields = fields;
	}

	public LinkedList<VariableJob> getVariables() {
		return variables;
	}

	public void setVariables(LinkedList<VariableJob> variables) {
		this.variables = variables;
	}

	public RefactoringLists() {
		methods = new LinkedList<MethodJob>();
		fields = new LinkedList<FieldJob>();
		variables = new LinkedList<VariableJob>();
	}
}
