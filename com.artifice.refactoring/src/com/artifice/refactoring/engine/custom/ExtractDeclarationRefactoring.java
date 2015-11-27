package com.artifice.refactoring.engine.custom;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Assignment.Operator;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;

public class ExtractDeclarationRefactoring extends Refactoring{

	private ICompilationUnit fUnit = null;
	private TextFileChange fChange = null;
	
	private ASTNode type = null;
	private ASTNode name = null;
	private ASTNode initializer = null;
	
	@SuppressWarnings("rawtypes")
	private List modifier = null;
	
	private VariableDeclarationFragment currentFragment = null;
	
	public ExtractDeclarationRefactoring(VariableDeclarationFragment node, ICompilationUnit unit) {
		this.currentFragment = node;
		this.fUnit = unit;
	}
	
	//MAIN
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor arg0)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		
		this.getData();
		
		return status;
	}
	
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor arg0)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		
		if(currentFragment.getParent() instanceof VariableDeclarationStatement)
			status.merge(this.rewriteExpressionStatement(status));
		
		if(currentFragment.getParent() instanceof VariableDeclarationExpression)
			status.merge(this.rewriteForNodeInitializer(status));
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor arg0) throws CoreException,
			OperationCanceledException {
		return fChange;
	}

	
	//REFACTORING
	/**
	 * Splits a variable initialization into two parts: Declaration and Initialization:
	 * i.e., int a = 1; ---> int a; a = 1; 
	 * @param status
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private RefactoringStatus rewriteExpressionStatement(RefactoringStatus status) {
		//Preparation
		ASTNode block = this.getLastBlock(this.currentFragment);
		
		AST ast = block.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		
		ListRewrite listRewrite = null;
		if(block instanceof SwitchStatement)
			listRewrite = rewrite.getListRewrite(block, SwitchStatement.STATEMENTS_PROPERTY);
		else 
			listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
		
		//Rewriting
		//Simple Declaration
		VariableDeclarationFragment newFragment = ast.newVariableDeclarationFragment();
		newFragment.setName((SimpleName) ASTNode.copySubtree(ast, this.name));
		
		VariableDeclarationStatement newStatement = ast.newVariableDeclarationStatement(newFragment);
		newStatement.setType((Type) ASTNode.copySubtree(ast, this.type));
		newStatement.modifiers().addAll(ASTNode.copySubtrees(ast, this.modifier));
		
		//Initialization
		Assignment newAssignment = ast.newAssignment();
		newAssignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, this.name));
		newAssignment.setRightHandSide((Expression) ASTNode.copySubtree(ast, this.initializer));
		newAssignment.setOperator(Operator.ASSIGN);
		
		ExpressionStatement newExpression = ast.newExpressionStatement(newAssignment);
		
		listRewrite.insertBefore(newStatement, currentFragment.getParent(), null);
		rewrite.replace(currentFragment.getParent(), newExpression, null);
		
		this.rewriteAST(rewrite);
		return status;
	}
	
	@SuppressWarnings("unchecked")
	private RefactoringStatus rewriteForNodeInitializer(RefactoringStatus status) {
		//Preparation
		ASTNode block = this.getLastBlock(this.currentFragment);
				
		AST ast = block.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
				
		ListRewrite listRewrite = null;
		if(block instanceof SwitchStatement)
			listRewrite = rewrite.getListRewrite(block, SwitchStatement.STATEMENTS_PROPERTY);
		else 
			listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
		//Rewrite
		//Simple Declaration
		VariableDeclarationFragment newFragment = ast.newVariableDeclarationFragment();
		newFragment.setName((SimpleName) ASTNode.copySubtree(ast, this.name));
			
		VariableDeclarationStatement newStatement = ast.newVariableDeclarationStatement(newFragment);
		newStatement.setType((Type) ASTNode.copySubtree(ast, this.type));
		newStatement.modifiers().addAll(ASTNode.copySubtrees(ast, this.modifier));
			
		//ForNode Initializer
		Assignment newAssignment = ast.newAssignment();
		newAssignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, this.name));
		newAssignment.setRightHandSide((Expression) ASTNode.copySubtree(ast, this.initializer));
		newAssignment.setOperator(Operator.ASSIGN);
		
		//End
		//If block needed before ForNode
		ForStatement forNode = (ForStatement) currentFragment.getParent().getParent();
		ASTNode parent = forNode.getParent();
		
		if((parent instanceof ForStatement) || (parent instanceof WhileStatement) || (parent instanceof DoStatement) || (parent instanceof IfStatement)) {
			Block newBlock = ast.newBlock();
			newBlock.statements().add(newStatement);
			
			ForStatement newForNode = ast.newForStatement();
			newForNode = (ForStatement) ASTNode.copySubtree(ast, forNode);
			newForNode.initializers().remove(0);
			newForNode.initializers().add(newAssignment);
			
			
			newBlock.statements().add(newForNode);
			
			if(parent instanceof ForStatement)
				rewrite.replace(((ForStatement)parent).getBody(), newBlock, null);
			else if(parent instanceof WhileStatement)
				rewrite.replace(((WhileStatement)parent).getBody(), newBlock, null);
			else if(parent instanceof DoStatement)
				rewrite.replace(((DoStatement)parent).getBody(), newBlock, null);
			
			else if(parent instanceof IfStatement) {
				IfStatement ifNode = (IfStatement) parent;
				if(forNode.equals(ifNode.getElseStatement()))
					rewrite.replace(ifNode.getElseStatement(), newBlock, null);
				else if (forNode.equals(ifNode.getThenStatement()))
					rewrite.replace(ifNode.getThenStatement(), newBlock, null);
			}
		}
		
		else {
		listRewrite.insertBefore(newStatement, forNode, null);
		rewrite.replace(currentFragment.getParent(), newAssignment, null);
		}
		
		this.rewriteAST(rewrite);
		return status;
	}
	
	
	//MISC
	private void getData() {
		if(currentFragment.getParent() instanceof VariableDeclarationStatement) {			//ExpressionStatement
			VariableDeclarationStatement statement = (VariableDeclarationStatement)currentFragment.getParent();
			this.type = statement.getType();
			this.modifier = statement.modifiers();
		}
		
		else if(currentFragment.getParent() instanceof VariableDeclarationExpression) {			//ForNode
			VariableDeclarationExpression expression = (VariableDeclarationExpression)currentFragment.getParent();
			this.type = expression.getType();
			this.modifier = expression.modifiers();
		}
		
		this.name = currentFragment.getName();
		this.initializer = currentFragment.getInitializer();
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
	
	private ASTNode getLastBlock(ASTNode node) {
		ASTNode block = node;

		while((!(block instanceof SwitchStatement)) && (!(block instanceof Block)))
				block = block.getParent();
		
		return block;
	}
	
	@Override
	public String getName() {
		return "Extract Declaration";
	}

}
