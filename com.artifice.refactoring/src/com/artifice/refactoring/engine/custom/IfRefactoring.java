package com.artifice.refactoring.engine.custom;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;

import com.artifice.refactoring.log.Logger;
import com.artifice.refactoring.log.LoggingUnit;

public class IfRefactoring extends Refactoring{

	private ICompilationUnit fUnit = null;
	private TextFileChange fChange = null;
	
	private IfStatement ifStatement = null;
	private ASTNode variable = null;
	private Expression thenExpression = null;
	private Expression elseExpression = null;
	private Expression ifExpression = null;
	
	
	public IfRefactoring(IfStatement statement) {
		this.ifStatement = statement;
	}
	
	//MAIN
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor arg0)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		
		//CompilationUnit
		ASTNode tmp = ifStatement.getParent();
		while (!(tmp instanceof MethodDeclaration))
				tmp = tmp.getParent();
		
		IMethod method = (IMethod)((MethodDeclaration)tmp).resolveBinding().getJavaElement();
		fUnit = method.getCompilationUnit();
		
		//Misc data
		this.getData();
		
		return status;
	}
	
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor arg0)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		
		status.merge(this.rewriteIfStatement(status));
		
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor arg0) throws CoreException,
			OperationCanceledException {
		return fChange;
	}

	@Override
	public String getName() {
		return "Shorten IfStatement";
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
	
	
	
	
	//REFACTORINGS
	/**
	 * Rewrites an IfStatement into the shortened form
	 * @param status
	 * @return
	 */
	private RefactoringStatus rewriteIfStatement(RefactoringStatus status) {
		
		AST ast = this.ifStatement.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		
		//create ConditionalExpression
		ConditionalExpression conditional = ast.newConditionalExpression();
		conditional.setExpression((Expression)ASTNode.copySubtree(ast, this.ifExpression));
		conditional.setThenExpression((Expression)ASTNode.copySubtree(ast, this.thenExpression));
		conditional.setElseExpression((Expression)ASTNode.copySubtree(ast, this.elseExpression));
		
		//create Assignment
		Assignment newAssignment = ast.newAssignment();
		newAssignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, this.variable));
		newAssignment.setRightHandSide(conditional);
		
		//Replace IfNode
		ExpressionStatement statement = ast.newExpressionStatement(newAssignment);
		rewrite.replace(this.ifStatement, statement, null);
		
		Logger.addUnit(fUnit.getElementName(), new LoggingUnit(this.ifStatement.getStartPosition(), this.ifStatement + " >>> " + statement), Logger.CONDITIONAL_REFACTORING);
		
		this.rewriteAST(rewrite);
		return status;
	}
	
	
	
	//MISC
	/**
	 * Sets the basic data needed for the the refactoring
	 */
	private void getData() {
		Assignment thenAssignment = (Assignment)((ExpressionStatement)this.ifStatement.getThenStatement()).getExpression();
		Assignment elseAssignment = (Assignment)((ExpressionStatement)this.ifStatement.getElseStatement()).getExpression();
		
		this.ifExpression = this.ifStatement.getExpression();
		this.variable = thenAssignment.getLeftHandSide();
		this.thenExpression = thenAssignment.getRightHandSide();
		this.elseExpression = elseAssignment.getRightHandSide();
		
	}
	
	
	
}
