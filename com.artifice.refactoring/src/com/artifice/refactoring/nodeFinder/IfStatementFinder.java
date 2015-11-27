package com.artifice.refactoring.nodeFinder;

import java.util.LinkedList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import com.artifice.refactoring.engine.Refactor;
import com.artifice.refactoring.engine.custom.ConditionalRefactoring;
import com.artifice.refactoring.engine.custom.IfRefactoring;

public class IfStatementFinder extends ASTVisitor{
	
	private LinkedList<IfStatement> ifStatements = new LinkedList<IfStatement>();
	private LinkedList<ConditionalExpression> conditionalExpressions = new LinkedList<ConditionalExpression>();
	
	public IfStatementFinder() {
		super();
	}
	
	@Override
	/**
	 * initiates refactoring for refactorable IfStatements
	 */
	public boolean visit(IfStatement node) {
		if(isRefactorable(node)) {
			this.ifStatements.add(node);
		}
		return super.visit(node);
	}
	
	@Override
	public boolean visit(ConditionalExpression node) {
		this.conditionalExpressions.add(node);
		return super.visit(node);
	}
	
	
	
	/**
	 * IfStatement is refactorable :
	 * 		- Then/Else Statement exist
	 * 		- Both are one-liners and of type Assignment/VariableDeclarationStatement
	 * 		- LeftHandSide of both Assignments are the same
	 * @param node IfStatement
	 * @return
	 */
	private boolean isRefactorable(IfStatement node) {
		boolean refactorable = false;
		
		//Then and Else must exist
		ASTNode thenNode = node.getThenStatement();
		
		if(node.getElseStatement() == null)
			return false;
		ASTNode elseNode = node.getElseStatement();
		
		// ExpressionStatement
		if((thenNode instanceof ExpressionStatement) && (elseNode instanceof ExpressionStatement)) {
			ExpressionStatement thenExpression = (ExpressionStatement)thenNode;
			ExpressionStatement elseExpression = (ExpressionStatement)elseNode;
			
			if((thenExpression.getExpression() instanceof Assignment) && (elseExpression.getExpression() instanceof Assignment)){
				Assignment thenAssignment = (Assignment)thenExpression.getExpression();
				Assignment elseAssignment = (Assignment)elseExpression.getExpression();
				
				if(thenAssignment.getLeftHandSide().toString().equals(elseAssignment.getLeftHandSide().toString()))
						return true;
			}
		}
		
		//VariableDeclarationStatement
		if((thenNode instanceof VariableDeclarationStatement) && (elseNode instanceof VariableDeclarationStatement)) {
			
		}
		
		return refactorable;
	}

	
	/**
	 * Refactors all objects from both lists.
	 */
	public void refactorLists() {
		ConditionalExpression conditional;
		IfStatement ifStatement;
		
		int startIf = 0;
		int startConditional = 0;
		
		while((!ifStatements.isEmpty()) || (!conditionalExpressions.isEmpty())) {
			
			// retrieves last objects
			ifStatement = ifStatements.pollLast();
			conditional = conditionalExpressions.pollLast();
			
			// get offset of both elements
			startIf = (ifStatement != null) ? ifStatement.getStartPosition() : -1;
			startConditional = (conditional != null) ? conditional.getStartPosition() : -1;
			
			//the assignment with the highest start position will be changed first
			if ((startIf == -1) && (startConditional != -1))					//no more expansions
				this.refactorConditinal(conditional);
			else if ((startIf != -1) && (startConditional == -1))			//no more contractions
				this.refactorIf(ifStatement);
			
			else {																	//both lists contained an item
				if(startIf > startConditional) {								//Expansion first
					this.refactorIf(ifStatement);
					this.conditionalExpressions.addLast(conditional);
				} else {															//Contraction first
					this.refactorConditinal(conditional);
					this.ifStatements.addLast(ifStatement);
				}
			}
		}
	}
	
	private void refactorIf(IfStatement node) {
		System.out.println("Shorten IfStatement : " + node);
		IfRefactoring refactoring = new IfRefactoring(node);
		Refactor.refactor(refactoring);
		
		Refactor.count[4]++;
	}
	
	private void refactorConditinal(ConditionalExpression node) {
		System.out.println("Expand ConditionalExpression : " + node.getParent());
		ConditionalRefactoring refactoring = new ConditionalRefactoring(node);
		Refactor.refactor(refactoring);
		
		Refactor.count[4]++;
	}
}
