package com.artifice.refactoring.engine.custom;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;

import com.artifice.refactoring.engine.RefactoredObjects;
import com.artifice.refactoring.log.Logger;
import com.artifice.refactoring.log.LoggingUnit;

public class LoopRefactoring extends Refactoring{

	private ICompilationUnit fUnit = null;
	private TextFileChange fChange = null;
	
	private ForStatement forStatement = null;
	private WhileStatement whileStatement = null;
	
	private ASTNode initializer = null; // (VariableDeclarationExpression or Assignment)
	private Expression boolExpression = null;
	private Expression updater = null;
	
	public LoopRefactoring(ForStatement forStatement, ICompilationUnit unit) {
		this.forStatement = forStatement;
		this.fUnit = unit;
	}
	
	public LoopRefactoring(WhileStatement whileStatement, ICompilationUnit unit) {
		this.whileStatement = whileStatement;
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
		
		if(forStatement != null)
			status.merge(this.rewriteForStatement(status));
		else if(whileStatement != null)
			status.merge(this.rewriteWhileStatement(status));
		else
			status.addError("Refactoring not properly initialized");
		
		return status;
	}
	
	@Override
	public Change createChange(IProgressMonitor arg0) throws CoreException,
			OperationCanceledException {
		return fChange;
	}
	
	@Override
	public String getName() {
		return "Transform Loop";
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
	 * Replaces the for-loop with a new while loop with the same logic
	 * @param status
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private RefactoringStatus rewriteForStatement(RefactoringStatus status){
		
		ASTNode block = this.forStatement;
		while((!(block instanceof SwitchStatement)) && (!(block instanceof Block)))
			block = block.getParent();
		
		AST ast = block.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		
		ListRewrite listRewrite;
		if(block instanceof SwitchStatement)
			listRewrite = rewrite.getListRewrite(block, SwitchStatement.STATEMENTS_PROPERTY);
		else 
			listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
		
		//WhileStatement
		WhileStatement newWhile = ast.newWhileStatement();
		
		//Bool expression
		if (boolExpression != null)
			newWhile.setExpression((Expression) ASTNode.copySubtree(ast, this.boolExpression));
		else 
			newWhile.setExpression(ast.newBooleanLiteral(true));
		
		//new Body
		ExpressionStatement updaterExpression = null;;
		if(this.updater != null)
			updaterExpression = ast.newExpressionStatement((Expression) ASTNode.copySubtree(ast, this.updater));
		
		ASTNode body = ASTNode.copySubtree(ast, forStatement.getBody());
			
		if(body instanceof Block) {									//Old block with updater
			if (this.updater != null)
				((Block) body).statements().add(updaterExpression);
			
			newWhile.setBody((Statement) body);						
		} else {													//New block with oldStatment and updater
			Block newBlock = ast.newBlock();
			newBlock.statements().add(body);
			
			if(this.updater != null)
				newBlock.statements().add(updaterExpression);
			
			newWhile.setBody(newBlock);
		}
		
		//Initializer
		if((this.initializer) != null && (block instanceof Block)) {
			ExpressionStatement initExpression = ast.newExpressionStatement((Expression) ASTNode.copySubtree(ast, this.initializer));
		
			ASTNode statementBeforeBlock = forStatement;
			while(!(statementBeforeBlock.getParent() instanceof Block))
				statementBeforeBlock = statementBeforeBlock.getParent();
			
			listRewrite.insertBefore(initExpression, statementBeforeBlock, null);
			
		}
		else if ((this.initializer) != null && (block instanceof SwitchStatement)) {
			ExpressionStatement initExpression = ast.newExpressionStatement((Expression) ASTNode.copySubtree(ast, this.initializer));
			listRewrite.insertBefore(initExpression, forStatement, null);
		}
		
		rewrite.replace(forStatement, newWhile, null);
		
		//Add to refactoredObjects so the node won't be edited anymore
		RefactoredObjects.addLoop(this.fUnit, newWhile);
		
		Logger.addUnit(fUnit.getElementName(), new LoggingUnit(forStatement.getStartPosition(), forStatement + " >>> " + newWhile), Logger.LOOP_REFACTORING);
		
		this.rewriteAST(rewrite);
		return status;
	}
	
	private RefactoringStatus rewriteWhileStatement(RefactoringStatus status){
		
		AST ast = whileStatement.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		
		ForStatement newFor = ast.newForStatement();
		newFor.setExpression((Expression) ASTNode.copySubtree(ast, this.boolExpression));
		newFor.setBody((Statement) ASTNode.copySubtree(ast, whileStatement.getBody()));
		
		rewrite.replace(whileStatement, newFor, null);
		
		//Add to refactoredObjects so the node won't be edited anymore
		RefactoredObjects.addLoop(this.fUnit, newFor);
		
		Logger.addUnit(fUnit.getElementName(), new LoggingUnit(whileStatement.getStartPosition(), whileStatement + " >>> " + newFor), Logger.LOOP_REFACTORING);
		
		this.rewriteAST(rewrite);
		return status;
	}
	
	//MISC
	/**
	 * Sets basic data for refactoring
	 */
	private void getData() {
		if(forStatement != null) {
			if (forStatement.initializers().size() > 0)
				this.initializer = (ASTNode) forStatement.initializers().get(0);
			
			if (forStatement.getExpression() != null)
				this.boolExpression = forStatement.getExpression();
			
			if(forStatement.updaters().size() > 0)
				this.updater = (Expression) forStatement.updaters().get(0);
		} 
		else if(whileStatement != null) {
			this.boolExpression = whileStatement.getExpression();
		}
	}

}
