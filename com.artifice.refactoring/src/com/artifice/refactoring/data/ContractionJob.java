package com.artifice.refactoring.data;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.SimpleName;

public class ContractionJob {
	
	private Assignment assignment = null;
	public Assignment getAssignment() {
		return this.assignment;
	}
	
	private SimpleName variable = null;
	public SimpleName getVariable() {
		return this.variable;
	}
	
	public ContractionJob(Assignment assignment, SimpleName variable) {
		this.assignment = assignment;
		this.variable = variable;
	}
	

	
}
