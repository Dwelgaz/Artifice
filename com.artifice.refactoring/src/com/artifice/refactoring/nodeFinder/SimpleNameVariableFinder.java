package com.artifice.refactoring.nodeFinder;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression.Operator;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;

public class SimpleNameVariableFinder extends ASTVisitor{
	private SimpleName leftSideVariable = null;
	private SimpleName rightSightVariable = null;
	
	public SimpleNameVariableFinder(SimpleName left) {
		this.leftSideVariable = left;
	}
	
	/**
	 * Visits variable nodes and checks if it is inside brackets,
	 * invokes a method, or is a variable of another class (qualified).
	 * Sets refactorable true if none of the mentioned is true;
	 */
	@Override
	public boolean visit(SimpleName node) {
		// Assignment is as contraction refactorable if there's the same varibale on both sides
		// and the variable in brackets
		if ((node.toString()).equals(leftSideVariable.toString())) {
			ASTNode temp = node;
			boolean flag = true;
			
			// Scan if any parent of variable is a ParenthesizedExpression
			while(!(temp instanceof Assignment)) {
				temp = temp.getParent();
				if((temp instanceof ParenthesizedExpression) || (temp instanceof MethodInvocation) || (temp instanceof QualifiedName) || (hasNOTOperator(temp))){
					flag = false;
					break;
				}
			}
			
			if (flag)
				this.rightSightVariable = node;
			
		}
		
		return super.visit(node);
	}
	
	public SimpleName getVariable() {
		return this.rightSightVariable;
	}
	
	private boolean hasNOTOperator(ASTNode node) {
		if(node instanceof PrefixExpression) {
			node = (PrefixExpression)node;
			if (((PrefixExpression) node).getOperator().equals(Operator.NOT))
				return true;
		}
		return false;
	}
}
