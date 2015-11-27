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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;

import com.artifice.refactoring.log.Logger;
import com.artifice.refactoring.log.LoggingUnit;

public class ConditionalRefactoring extends Refactoring{

	private TextFileChange fChange = null;
	private ICompilationUnit fUnit = null;
	
	private ConditionalExpression conditional = null;
	private ASTNode variable = null;
	private ASTNode type = null;
	private Expression thenExpression = null;
	private Expression elseExpression = null;
	private Expression ifExpression = null;

	public ConditionalRefactoring(ConditionalExpression cond) {
		this.conditional = cond;
	}
	
	
	//MAIN
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor arg0)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		
		//CompilationUnit
		ASTNode tmp = conditional.getParent();
		while (!(tmp instanceof MethodDeclaration))
			tmp = tmp.getParent();
				
		IMethod method = (IMethod)((MethodDeclaration)tmp).resolveBinding().getJavaElement();
		fUnit = method.getCompilationUnit();
		
		//basic data
		this.getData();
		
		return status;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor arg0)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		
		if (this.type == null)
			status.merge(this.rewriteCondExpressionWithAssignment(status));
		else
			status.merge(this.rewriteCondExpressionWithDeclaration(status));
		
		return status;
	}
	
	@Override
	public Change createChange(IProgressMonitor arg0) throws CoreException,
			OperationCanceledException {
		return fChange;
	}

	@Override
	public String getName() {
		return "Expand ConditionalExpression";
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
	
	
	
	
	//REFACTORING
	/**
	 * Expands the conditionalExpression to an IfStatement(with Assignments)
	 * @param status
	 * @return
	 */
	private RefactoringStatus rewriteCondExpressionWithAssignment(RefactoringStatus status) {
		ASTNode tmp = conditional;
		while (!(tmp instanceof ExpressionStatement))
				tmp = tmp.getParent();
		
		AST ast = tmp.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);

		IfStatement statement = this.createIfStatement(ast);
		rewrite.replace(tmp, statement, null);
		
		Logger.addUnit(fUnit.getElementName(), new LoggingUnit(tmp.getStartPosition(), tmp.toString() + " >>> " + statement), Logger.CONDITIONAL_REFACTORING);
		
		this.rewriteAST(rewrite);
		return status;
	}
	
	/**
	 * Expands the conditionalExpression to an IfStatement(with VariableDeclaration)
	 * @param status
	 * @return
	 */
	private RefactoringStatus rewriteCondExpressionWithDeclaration(RefactoringStatus status) {
		ASTNode tmp = conditional;
		while (!(tmp instanceof VariableDeclarationStatement))
				tmp = tmp.getParent();
		
		ASTNode block = tmp;
		while(!(block instanceof Block))
			block = block.getParent();
		
		AST ast = block.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		
		ListRewrite listRewrite = rewrite.getListRewrite((Block)block, Block.STATEMENTS_PROPERTY);
		
		//Variable declaration
		VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		fragment.setName((SimpleName)ASTNode.copySubtree(ast, variable));
		
		VariableDeclarationStatement declaration = ast.newVariableDeclarationStatement(fragment);
		declaration.setType((Type)ASTNode.copySubtree(ast, this.type));
		
		listRewrite.insertBefore(declaration, tmp, null);
		//IfStatement
		IfStatement statement = this.createIfStatement(ast);
		rewrite.replace(tmp, statement, null);
		
		Logger.addUnit(fUnit.getElementName(), new LoggingUnit(tmp.getStartPosition(), tmp + " >>> " + declaration + "\n" + statement), Logger.CONDITIONAL_REFACTORING);
		
		this.rewriteAST(rewrite);
		return status;
	}
	
	
	
	//MISC
	/**
	 * Sets the basic data needed for the the refactoring
	 */
	private void getData() {	
		this.ifExpression = conditional.getExpression();
		
		if (conditional.getParent() instanceof Assignment)	//
			this.variable = ((Assignment)conditional.getParent()).getLeftHandSide();
		
		else  {	//if not Assignment is has to be VariableDeclarationFragemnt
			VariableDeclarationFragment fragment = (VariableDeclarationFragment)conditional.getParent();
			this.variable = fragment.getName();
			this.type = ((VariableDeclarationStatement)fragment.getParent()).getType();			
		}
		
		this.thenExpression = conditional.getThenExpression();
		this.elseExpression = conditional.getElseExpression();
		
	}
	
	/**
	 * Creates the ifStatement based on the conditionalExpression
	 * @param ast
	 * @return
	 */
	private IfStatement createIfStatement(AST ast) {
		IfStatement newIfStatement = ast.newIfStatement();
		
		//ElseStatement
		Assignment elseAssignment = ast.newAssignment();
		elseAssignment.setRightHandSide((Expression) ASTNode.copySubtree(ast, elseExpression));
		elseAssignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, variable));
		newIfStatement.setElseStatement(ast.newExpressionStatement(elseAssignment));
		
		//ThenStatement
		Assignment thenAssignment = ast.newAssignment();
		thenAssignment.setRightHandSide((Expression) ASTNode.copySubtree(ast, thenExpression));
		thenAssignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, variable));
		newIfStatement.setThenStatement(ast.newExpressionStatement(thenAssignment));
		
		//If Expression
		newIfStatement.setExpression((Expression) ASTNode.copySubtree(ast, ifExpression));
		
		return newIfStatement;
	}
	
}
