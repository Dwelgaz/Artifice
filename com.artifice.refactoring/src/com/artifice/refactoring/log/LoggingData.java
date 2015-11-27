package com.artifice.refactoring.log;

import java.util.Collections;
import java.util.LinkedList;

public class LoggingData {

	private LinkedList<LoggingUnit > conditionals;	// If and Conditional
	private LinkedList<LoggingUnit > assignments;	// Contraction/Expansion
	private LinkedList<LoggingUnit > loops;
	private LinkedList<LoggingUnit > crements;
	private LinkedList<LoggingUnit > methodRenames;
	private LinkedList<LoggingUnit > variableRenames;
	
	public LoggingData() {
		this.conditionals = new LinkedList<LoggingUnit >();	// If and Conditional
		this.assignments = new LinkedList<LoggingUnit >();	// Contraction/Expansion
		this.loops = new LinkedList<LoggingUnit >();
		this.crements = new LinkedList<LoggingUnit >();
		this.methodRenames = new LinkedList<LoggingUnit >();
		this.variableRenames = new LinkedList<LoggingUnit >();
	}
	
	//ADDING
	public void addConditional(LoggingUnit unit) {
		this.conditionals.add(unit);
	}
	
	public void addAssignment(LoggingUnit unit) {
		this.assignments.add(unit);
	}
	
	public void addLoop(LoggingUnit unit) {
		this.loops.add(unit);
	}
	
	public void addCrements(LoggingUnit unit) {
		this.crements.add(unit);
	}
	
	public void addMethod(LoggingUnit unit) {
		this.methodRenames.add(unit);
	}
	
	public void addVariable(LoggingUnit unit) {
		this.variableRenames.add(unit);
	}
	
	
	
	@Override
	public String toString() {
		this.sortLists();
		
		String result = "";
		
		result += ">>> Conditional and IfStatement Transformations" + System.getProperty("line.separator");
		for(LoggingUnit unit : this.conditionals)
			result += unit;
		
		result += ">>> Expansions and Contractions" + System.getProperty("line.separator");
		for(LoggingUnit unit : this.assignments)
			result += unit;
		
		result += ">>> For/While Transformation" + System.getProperty("line.separator");
		for(LoggingUnit unit : this.loops)
			result += unit;
		
		result += ">>> Expansion of Increments/Decrements" + System.getProperty("line.separator");
		for(LoggingUnit unit : this.crements)
			result += unit;
		
		result += ">>> Renamed Variables" + System.getProperty("line.separator");
		for(LoggingUnit unit : this.variableRenames)
			result += unit;
		
		result += ">>> Renamed Methods" + System.getProperty("line.separator");
		for(LoggingUnit unit : this.methodRenames)
			result += unit;
		
		return result;
	}
	
	public void sortLists() {
		Collections.sort(conditionals);
		Collections.sort(assignments);
		Collections.sort(loops);
		Collections.sort(crements);
		Collections.sort(variableRenames);
		Collections.sort(methodRenames);
	}
	
}
