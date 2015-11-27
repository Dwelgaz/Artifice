package com.artifice.refactoring.nodeFinder;


import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;

import com.artifice.refactoring.engine.Refactor;
import com.artifice.refactoring.engine.RefactoredObjects;
import com.artifice.refactoring.engine.custom.CrementRefactoring;

public class CrementFinder extends ASTVisitor{
	
	private boolean unitChanged = false;
	public boolean hasChanged() {
		return unitChanged;
	}
	
	public CrementFinder() {
		super();
	}
	
	@Override
	/**
	 * Finds a suitable variable, that can be changed.
	 * If change is excekuted, no other expression will be changed, until a new visitor object
	 * is created (or else, the unit may contain errors)
	 */
	public boolean visit(SimpleName variable) {
		if (!unitChanged) {
			ASTNode parent = variable.getParent();
		
			if (parent instanceof FieldAccess)
				parent = parent.getParent();
		
			//--x, ++x
			if(parent instanceof PrefixExpression) {
				PrefixExpression prefix = (PrefixExpression)parent;
			
				if((prefix.getOperator().equals(PrefixExpression.Operator.INCREMENT)) || (prefix.getOperator().equals(PrefixExpression.Operator.DECREMENT))) {
					if (this.isRefactorable(prefix)) {
						System.out.println("Transform Increment/Decrement : " + variable.getParent());
						CrementRefactoring refactoring = new CrementRefactoring(variable);
						Refactor.refactor(refactoring);
						unitChanged = true;
						
						Refactor.count[1]++;
					}
				}
			}
		
			//x++, x--
			if(parent instanceof PostfixExpression) {
				PostfixExpression postfix = (PostfixExpression)parent;
			
				if((postfix.getOperator().equals(PostfixExpression.Operator.INCREMENT)) || (postfix.getOperator().equals(PostfixExpression.Operator.DECREMENT))) {
					if (this.isRefactorable(postfix)) {
						System.out.println("Transform Increment/Decrement : " + variable.getParent());
						CrementRefactoring refactoring = new CrementRefactoring(variable);
						Refactor.refactor(refactoring);
						unitChanged = true;
						
						Refactor.count[1]++;
					}
				}
			}
			
			
		}
		return super.visit(variable);
	}
	
	public boolean isRefactorable(ASTNode variable) {
		ASTNode body = this.getBodyNode(variable);
		
		if (variable.getParent() instanceof ExpressionStatement)  {
//			System.out.println(variable.getParent().getStartPosition() + " " + variable.getParent());
			
			if (!RefactoredObjects.containsCrement(variable.getParent().getStartPosition(), variable.getParent()))
				return true;
			return false;
		}
		
		else if (body instanceof ExpressionStatement)
			return true;
		else if (body instanceof IfStatement)
			return true;
		else if (body instanceof ForStatement)
			return true;
		
		else
			return false;	
	}
	
	
	/**
	 * Gets the last node before the BlockStatement
	 * @return ASTNode
	 */
	private ASTNode getBodyNode(ASTNode node) {
		ASTNode bodyNode = node.getParent();
		while(!(bodyNode.getParent() instanceof Block)) {
			bodyNode = bodyNode.getParent();
		}
		return bodyNode;
	}
	
}
