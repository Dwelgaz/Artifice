package com.artifice.refactoring.engine;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class RefactoredObjects {

	/*
	 * Some transformation have to be saved, so that no other refactoring reverses the effect.
	 * For Example:
	 * 		- For loop will be transformed into while loop
	 * 		- CompilationUnit update
	 * 		- Transformed while loop will be transformed into for-loop
	 * 		...
	 * 
	 * 	Increment/Decrement will be saved, if it is an ExpressionStatement with a Prefix/Postfix expression
	 */

	// Loops
	private static HashMap<ICompilationUnit, HashSet<String>> refactoredLoops = new HashMap<ICompilationUnit, HashSet<String>>();
	
	public static void addLoop(ICompilationUnit unit, ASTNode node) {
		
		if(refactoredLoops.containsKey(unit)) {
			if(refactoredLoops.get(unit) != null) {
				HashSet<String> hash = refactoredLoops.get(unit);
				if(!(hash.contains(node.toString())))
					hash.add(createLoopString(node));	
			}
			
		} else {
			HashSet<String> hash = new HashSet<String>();
			hash.add(createLoopString(node));
			refactoredLoops.put(unit, hash);
		}

	}
	
	public static boolean containsLoop(ICompilationUnit unit, ASTNode node) {
		if(refactoredLoops.containsKey(unit)) {
			if(refactoredLoops.get(unit) != null) {
				HashSet<String> hash = refactoredLoops.get(unit);
				
				String loopStr = "";
				
				if (node instanceof ForStatement) {
					ForStatement forNode = (ForStatement) node;
					loopStr = "" + forNode.initializers().toString() +";" + forNode.getExpression().toString() + ";" + forNode.updaters().toString();
				} else if (node instanceof WhileStatement) {
					WhileStatement whileNode = (WhileStatement) node;
					loopStr = whileNode.getExpression().toString();
				}
				
				if(hash.contains(loopStr))
					return true;
			}
		}
		return false;
	}
	
	private static String createLoopString(ASTNode node) {	
		if(node instanceof ForStatement) {
			ForStatement forNode = (ForStatement) node;
			return "" + forNode.initializers().toString() +";" + forNode.getExpression().toString() + ";" + forNode.updaters().toString();
		}
		else if(node instanceof WhileStatement) {
			WhileStatement whileNode = (WhileStatement) node;
			return whileNode.getExpression().toString();
		}
		return "";
	}
	
	// Increment/Decrement 
	
	private static HashMap<Integer, String> refactoredCrements = new HashMap<Integer, String>();
	
	public static void addCrement(int startPosition, ASTNode node) {
		refactoredCrements.put(startPosition, node.toString());
	}
	
	public static boolean containsCrement(int startPosition, ASTNode node) {	
		if (refactoredCrements.containsKey(startPosition))
			if(refactoredCrements.get(startPosition) != null)
				return refactoredCrements.get(startPosition).equals(node.toString());
		return false;
	}
	
	// Variable 
	
	private static HashMap<ICompilationUnit, HashSet<String>> refactoredVariables = new HashMap<ICompilationUnit, HashSet<String>>();
	
	public static void addVariable(ICompilationUnit unit , String name) {
		if(refactoredVariables.containsKey(unit)) {
			if(refactoredVariables.get(unit) != null) {
				HashSet<String> hash = refactoredVariables.get(unit);
				if(!(hash.contains(name)))
					hash.add(name);
			}
			
		} else {
			HashSet<String> hash = new HashSet<String>();
			hash.add(name);
			refactoredVariables.put(unit, hash);
		}
	}
	
	public static boolean containsVariable(ICompilationUnit unit, String name) {
		if (refactoredVariables.containsKey(unit))
			if (refactoredVariables.get(unit) != null) {
				HashSet<String> hash = refactoredVariables.get(unit);
				if(hash.contains(name))
					return true;
			}
		return false;
	}

	public static void clearSet() {
		refactoredLoops.clear();
		refactoredCrements.clear();
		refactoredVariables.clear();
	}
}
