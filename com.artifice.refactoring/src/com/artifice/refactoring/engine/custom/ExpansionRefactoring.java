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
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import com.artifice.refactoring.log.Logger;
import com.artifice.refactoring.log.LoggingUnit;

public class ExpansionRefactoring extends Refactoring{

	private Assignment fAssignment;
	private ICompilationUnit fUnit;
	
	
	
	/**
	 * @param assignment assignment which will be changed
	 * @param unit CU of the assignment
	 */
	public ExpansionRefactoring(Assignment assignment, ICompilationUnit unit) {
		super();
		this.fAssignment = assignment;
		this.fUnit = unit;
	}
	
	private TextFileChange fChange= null;
	
	
	
	//MAIN

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		
		if (!fUnit.exists())
			status.merge(RefactoringStatus.createFatalErrorStatus(fUnit.getElementName() + " doesn't exist"));
		if (fUnit.isReadOnly())
			status.merge(RefactoringStatus.createFatalErrorStatus(fUnit.getElementName() + " is read-only"));
		if (!fUnit.isStructureKnown())
			status.merge(RefactoringStatus.createFatalErrorStatus(fUnit + "s structure is unkown"));
		
		return status;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		status.merge(this.rewriteAssigmentNode(status));
		return status;
	}
	
	@Override
	public Change createChange(IProgressMonitor monitor) throws CoreException,
			OperationCanceledException {
		return fChange;
	}

	@Override
	/**
	 * Refactoring name
	 */
	public String getName() {
		return "Expand Assignment";
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

	//MISC
	/**
	 * Expands the given Assignment in the following form:
	 * old --> Variable += Expression
	 * new --> Variable = Variable + (Expression)
	 * @param status RefactoringStatus
	 * @return OK, if nothing failed
	 */
	private RefactoringStatus rewriteAssigmentNode( RefactoringStatus status) {
		AST node = fAssignment.getAST();
		Assignment newAssignment = node.newAssignment();
		
		//change old operator to "="
		newAssignment.setOperator(Assignment.Operator.ASSIGN);
		
		//Left Side
		newAssignment.setLeftHandSide((Expression)ASTNode.copySubtree(node, fAssignment.getLeftHandSide()));
		
		// Right Side (Variable + (oldExpression))
		InfixExpression newRightSide = node.newInfixExpression();
		
		// Right Side / Left Operand
		newRightSide.setLeftOperand((Expression)ASTNode.copySubtree(node, fAssignment.getLeftHandSide()));
		switch(fAssignment.getOperator().toString()) {
		case "+=" : newRightSide.setOperator(InfixExpression.Operator.PLUS);
					break;
		case "-=" : newRightSide.setOperator(InfixExpression.Operator.MINUS);
					break;
		case "/=" : newRightSide.setOperator(InfixExpression.Operator.DIVIDE);
					break;
		case "*=" : newRightSide.setOperator(InfixExpression.Operator.TIMES);
					break;
		}
		
		// Right Side / Right Operand (ParenthesizedExpression)
		if (fAssignment.getRightHandSide() instanceof ParenthesizedExpression) {
			newRightSide.setRightOperand((Expression)ASTNode.copySubtree(node, fAssignment.getRightHandSide()));
		} else {
			ParenthesizedExpression rightOperand = node.newParenthesizedExpression();
			rightOperand.setExpression((Expression)ASTNode.copySubtree(node, fAssignment.getRightHandSide()));
			newRightSide.setRightOperand(rightOperand);
		}
		
		// Complete new assignment
		newAssignment.setRightHandSide(newRightSide);
		
		// replacing oldAssignment
		ASTRewrite rewrite = ASTRewrite.create(node);
		rewrite.replace(fAssignment, newAssignment, null);
		
		Logger.addUnit(fUnit.getElementName(), new LoggingUnit(fAssignment.getStartPosition(), fAssignment + " >>> " + newAssignment), Logger.ASSIGNMENT_REFACTORING);
		
		this.rewriteAST(rewrite);
		return status;
	}
	

}
