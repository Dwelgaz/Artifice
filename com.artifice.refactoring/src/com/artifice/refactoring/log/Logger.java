package com.artifice.refactoring.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.HashMap;

import com.artifice.refactoring.engine.Refactor;

public class Logger {

	private static String project = "";
	
	public static void setProjectName(String name) {
		project = name;
	}
	
	public static final int CONDITIONAL_REFACTORING = 0;
	public static final int ASSIGNMENT_REFACTORING = 1;
	public static final int LOOP_REFACTORING = 2;
	public static final int CREMENT_REFACTORING = 3;
	public static final int VARIABLE_REFACTORING = 4;
	public static final int METHOD_REFACTORING = 5;
	
	private static HashMap<String, LoggingData> log; //<CompilationUnit string, loggingData>
	
	public static void initialize() {
		log = new HashMap<String, LoggingData>();
		
		Refactor.count[0] = 0;
		Refactor.count[1] = 0;
		Refactor.count[2] = 0;
		Refactor.count[3] = 0;
		Refactor.count[4] = 0;
	}
	
	public static void clearAll() {
		if (log != null)
			log.clear();
	}
	
	public static void addUnit(String compilationUnit, LoggingUnit unit, int type) {
		if (!log.containsKey(compilationUnit))
			log.put(compilationUnit, new LoggingData());
		
		if (log.containsKey(compilationUnit)) {
			switch (type) {
			case 0 : log.get(compilationUnit).addConditional(unit);
					 break;
			case 1 : log.get(compilationUnit).addAssignment(unit);
			 		 break;
			case 2 : log.get(compilationUnit).addLoop(unit);
					 break;
			case 3 : log.get(compilationUnit).addCrements(unit);
			 		 break;
			case 4 : log.get(compilationUnit).addVariable(unit);
	 		 		 break;
			case 5 : log.get(compilationUnit).addMethod(unit);
	 		 		 break;
			}
		}
		
	}
	
	public static void createLogFile() {
		Writer output = null;

		//Text
		String result = "Project : " + project + System.getProperty("line.separator");
		
		for(String compilationUnit : log.keySet()) {
			result += "--- Changes made to " + compilationUnit + " ---" + System.getProperty("line.separator") + System.getProperty("line.separator");
			result += log.get(compilationUnit).toString();
			result += System.getProperty("line.separator") + System.getProperty("line.separator") + System.getProperty("line.separator");
		}
		
		result += "Renaming : " + Refactor.count[0] + "; Expansion: " + Refactor.count[1] + "; Contraction : " + Refactor.count[2] +
				"; Loop : " + Refactor.count[3] + "; Conditional : " + Refactor.count[4];
		//File
		Calendar calendar = Calendar.getInstance();
		
		String fileName = (calendar.get(Calendar.MONTH)+1) + "_" + calendar.get(Calendar.DATE) + "-" + calendar.get(Calendar.HOUR_OF_DAY) + "_" + calendar.get(Calendar.MINUTE) + "-" + project;
		File file = new File(System.getProperty("user.home"), fileName + ".txt");
		
		try {
			output = new BufferedWriter(new FileWriter(file));
			output.write(result);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Obfuscation completed");
	}
	
}
