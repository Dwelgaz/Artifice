package com.artifice.refactoring.engine.custom;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;

import com.artifice.refactoring.data.ContractionJob;
import com.artifice.refactoring.engine.RefactoredObjects;
import com.artifice.refactoring.log.Logger;
import com.artifice.refactoring.log.LoggingUnit;

public class ContractionRefactoring extends Refactoring{

	private ContractionJob fAssignment = null;
	private ICompilationUnit fUnit = null;
	private TextFileChange fChange= null;
	
	public ContractionRefactoring(ContractionJob job, ICompilationUnit unit) {
		this.fAssignment = job;
		this.fUnit = unit;
	}
	
	
	
	
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor arg0)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		return status;
	}
	
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor arg0)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		
		if (this.shortenToCrement())
			status.merge(this.rewriteToCrement(status));
		else
			status.merge(this.rewriteVariableAssignment(status));
		
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor arg0) throws CoreException,
			OperationCanceledException {
		return fChange;
	}	
	
	@Override
	public String getName() {
		return "Shorten assignment";
	}
	
	private void rewriteAST(ASTRewrite rewrite) {
		try {			
			fChange= new TextFileChange(fUnit.getElementName(), (IFile) fUnit.getResource());
			fChange.setTextType("java");
			fChange.setEdit(rewrite.rewriteAST());

		} catch (MalformedTreeException exception) {
//			RefactoringPlugin.log(exception);
		} catch (IllegalArgumentException exception) {
//			RefactoringPlugin.log(exception);
		} catch (CoreException exception) {
//			RefactoringPlugin.log(exception);
		}
	}
	
	
	
	
	//Helper
		/**
		 * Replaces the Variable by 0 and changes the assignment operator
		 * to += or -=
		 * @param status
		 * @return
		 */
		private RefactoringStatus rewriteVariableAssignment(RefactoringStatus status) {
			
			AST node = fAssignment.getAssignment().getAST();
			ASTRewrite rewrite = ASTRewrite.create(node);
			
			Assignment oldAssignment = fAssignment.getAssignment();
			Assignment newAssignment = node.newAssignment();


			//New Node
			//Left side
			newAssignment.setLeftHandSide((Expression)ASTNode.copySubtree(node, oldAssignment.getLeftHandSide()));
			
			//Operator
			InfixExpression exp = (InfixExpression)fAssignment.getVariable().getParent();
			
			switch (exp.getOperator().toString()) {
				case "-" : 	newAssignment.setOperator(Assignment.Operator.MINUS_ASSIGN);
					   		break;
				case "+" : 	newAssignment.setOperator(Assignment.Operator.PLUS_ASSIGN);
		   					break;
			}
			
			// Change variable with 0 temporarily
			boolean side = false; //false left, true right
			exp = (InfixExpression)fAssignment.getVariable().getParent();
			if (exp.getLeftOperand().equals(fAssignment.getVariable())) {
				exp.setLeftOperand(node.newNumberLiteral("0"));
				side = false;
			}
			if (exp.getRightOperand().equals(fAssignment.getVariable())) {
				exp.setRightOperand(node.newNumberLiteral("0"));
				side = true;
			}
			
			//Right side
			newAssignment.setRightHandSide((Expression)ASTNode.copySubtree(node, oldAssignment.getRightHandSide()));
			
			//change 0 back to variable
			if (!side)
				exp.setLeftOperand(fAssignment.getVariable());
			else
				exp.setRightOperand(fAssignment.getVariable());
			
			//Change Operator
			rewrite.replace(oldAssignment, newAssignment, null);
			//Change variable on rightSide to 0
			
			Logger.addUnit(fUnit.getElementName(), new LoggingUnit(oldAssignment.getStartPosition(), oldAssignment + " >>> " + newAssignment), Logger.ASSIGNMENT_REFACTORING);
			
			this.rewriteAST(rewrite);
			return status;
		}
	
		/**
		 * Rewrites the Assignment to variable++ oder variable--
		 * @param status
		 * @return
		 */
		private RefactoringStatus rewriteToCrement(RefactoringStatus status) {
			
			AST node = fAssignment.getAssignment().getAST();
			ASTRewrite rewrite = ASTRewrite.create(node);
			
			Assignment oldAssignment = fAssignment.getAssignment();
			PostfixExpression newPostfix = node.newPostfixExpression();
			
			//Variable
			newPostfix.setOperand((Expression)ASTNode.copySubtree(node, fAssignment.getVariable()));
			
			//Operator
			switch (((InfixExpression)oldAssignment.getRightHandSide()).getOperator().toString()) {
				case "+" : 	newPostfix.setOperator(PostfixExpression.Operator.INCREMENT);
							break;
				case "-" : 	newPostfix.setOperator(PostfixExpression.Operator.DECREMENT);
							break;
			}
			
			ExpressionStatement newExpression = node.newExpressionStatement(newPostfix);
			
			if(oldAssignment.getParent() instanceof ForStatement) {			// ForStatement as updater
				rewrite.replace(oldAssignment, newPostfix, null);
				Logger.addUnit(fUnit.getElementName(), new LoggingUnit(oldAssignment.getStartPosition(), oldAssignment + " >>> " + newPostfix), Logger.ASSIGNMENT_REFACTORING);
				RefactoredObjects.addCrement(oldAssignment.getStartPosition(), newPostfix);
			}
			else {
				rewrite.replace(oldAssignment.getParent(), newExpression, null);
				Logger.addUnit(fUnit.getElementName(), new LoggingUnit(oldAssignment.getParent().getStartPosition(), oldAssignment + " >>> " + newExpression), Logger.ASSIGNMENT_REFACTORING);
				RefactoredObjects.addCrement(oldAssignment.getParent().getStartPosition(), newExpression);
			}
			
			this.rewriteAST(rewrite);
			return status;
		}
		

		/**
		 * Checks if the Assignment can be transformed to variable++ or variable--
		 * @return
		 */
		private boolean shortenToCrement() {
			Assignment assignment = fAssignment.getAssignment();
			boolean flag = false;
		
			if(assignment.getRightHandSide() instanceof InfixExpression) {
				InfixExpression infix = (InfixExpression)assignment.getRightHandSide();
			
				//String abfrage
				if(infix.toString().equals(fAssignment.getVariable() + " + 1"))
					flag = true;
				else if(infix.toString().equals(fAssignment.getVariable() + " + (1)"))
					flag = true;
				else if(infix.toString().equals(fAssignment.getVariable() + " - (1)"))
					flag = true;
				else if(infix.toString().equals(fAssignment.getVariable() + " - 1"))
					flag = true;
				else if(infix.toString().equals("(1) + " + fAssignment.getVariable()))
					flag = true;
				else if(infix.toString().equals("1 + " + fAssignment.getVariable()))
					flag = true;
			}
			return flag;
		}
	
	

}
