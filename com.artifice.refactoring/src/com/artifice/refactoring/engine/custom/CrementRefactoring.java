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
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
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

public class CrementRefactoring extends Refactoring{

	private SimpleName fVariable;
	private ICompilationUnit fUnit;
	
	private Operator operator = Operator.PLUS;
	private boolean isField = false;
	private int placement;
	
	
	private TextFileChange fChange = null;
	
	public CrementRefactoring(SimpleName variable) {
		super();
		this.fVariable = variable;
	}

	
	//MAIN
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor arg0)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		
		//Set ICompilationUnit
		ASTNode tmp = fVariable.getParent();
		while (!(tmp instanceof MethodDeclaration))
				tmp = tmp.getParent();
		
		IMethod method = (IMethod)((MethodDeclaration)tmp).resolveBinding().getJavaElement();
		fUnit = method.getCompilationUnit();
		
		//FieldAccess, Orientation and Operator
		this.isFieldAccess();
		this.setPlacement();
		this.setOperator();
		
		return status;
	}
	
	/**
	 * Checks which type of refactoring will be performed
	 */
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor arg0)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		
		ASTNode expression = null;
		//Prefix bzw. Postfixnode
		if(isField)
			expression = fVariable.getParent().getParent();
		else
			expression = fVariable.getParent();
		
		//bodyNode decides which special refactoring
		ASTNode bodyNode = this.getBodyNode();
		
		// if parent of expression is ExpressionStatement, expression must be x++;
		if (expression.getParent() instanceof ExpressionStatement) {							//i++
			status.merge(this.rewriteExpressionStatement(status, expression));
		} else if(bodyNode instanceof ForStatement) {											//for
			status.merge(this.rewriteForLoop(status, (ForStatement)bodyNode, expression));
		} else if (bodyNode instanceof WhileStatement) {										//while
			status.merge(this.rewriteWhileLoop(status, (WhileStatement)bodyNode, expression));
		} else if (bodyNode instanceof IfStatement) {											//If
			status.merge(this.rewriteIfStatement(status, (IfStatement)bodyNode, expression));
		} else { 																				//standard expression statements á la a = a++ +.... usw.
			status.merge(this.rewriteExpressionStatement(status, (ExpressionStatement)bodyNode, expression));
		}
		
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor arg0) throws CoreException,
			OperationCanceledException {
		return fChange;
	}

	@Override
	public String getName() {
		return "Expanding increments or decrements";
	}

	/**
	 * Writes Changes to the TextFileChange Object
	 * @param rewrite
	 */
	private void rewriteAST(ASTRewrite rewrite) {
		try {			
			fChange= new TextFileChange(fUnit.getElementName(), (IFile) fUnit.getResource());
			fChange.setTextType("java");
			fChange.setEdit(rewrite.rewriteAST());

		} catch (MalformedTreeException exception) {
//			RefactoringPlugin.log(exception);
		} catch (IllegalArgumentException exception) {
			exception.printStackTrace();
		} catch (CoreException exception) {
//			RefactoringPlugin.log(exception);
		}
	}
	
	
	
	
	//REFACTORINGS
	/**
	 * Rewrites an expression (variable++ and so an) to variable = variable + 1 (and so on)
	 * @param status	RefactoringStatus
	 * @param expression	Prefix or Postfix node
	 * @return RefactoringStatus
	 */
	private RefactoringStatus rewriteExpressionStatement(RefactoringStatus status, ASTNode expression) {
		
		AST ast = expression.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		
		//if byte, cast is necessary
		Expression exp = (Expression) expression;
		IBinding binding = exp.resolveTypeBinding();
		
		Expression newStatement = this.createAssignment(ast);
//		System.out.println(binding.getKey());
		if((binding.getKey().equals("Ljava/lang/Byte;")) || ((binding.getKey().equals("B")))) {
			Assignment newAssignment = (Assignment)newStatement;
			
			ParenthesizedExpression brackets = ast.newParenthesizedExpression();
			brackets.setExpression((Expression) ASTNode.copySubtree(ast, newAssignment.getRightHandSide()));
			
			CastExpression cast = ast.newCastExpression();
			cast.setExpression(brackets);
			cast.setType(ast.newPrimitiveType(PrimitiveType.BYTE));
			
			newAssignment.setRightHandSide(cast);
			newStatement = newAssignment;
		}
		
		//Rewriter
		rewrite.replace(expression, newStatement, null);
		Logger.addUnit(fUnit.getElementName(), new LoggingUnit(expression.getStartPosition(), expression + " >>> " + newStatement), Logger.CREMENT_REFACTORING);
		RefactoredObjects.addCrement(expression.getParent().getStartPosition(), newStatement);
		
		this.rewriteAST(rewrite);		
		return status;
	}
	
	/**
	 * Deletes an Increment/Decrement inside an ExpressionStatement, and creates a new assignemnt before or after
	 * the statement based on the placement
	 * @param status		RefactoringStatus
	 * @param statement		ExpressionStatement of in/decrement
	 * @param expression	Increment/Decrement Node
	 * @return	RefactoringStatus
	 */
	private RefactoringStatus rewriteExpressionStatement(RefactoringStatus status, ExpressionStatement statement, ASTNode expression) {
		
		Block body = this.getMethodDeclaration(statement);
		AST ast = body.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		ListRewrite listRewrite = rewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY);
		
		//place newAssignment before or after statement based on placement
		if (placement == -1)
			listRewrite.insertBefore(ast.newExpressionStatement(this.createAssignment(ast)), statement, null);
		else
			listRewrite.insertAfter(ast.newExpressionStatement(this.createAssignment(ast)), statement, null);
		
		// Replace Crement by variable
		PrefixExpression prefix = ast.newPrefixExpression();
		prefix.setOperand((Expression)ASTNode.copySubtree(ast, fVariable));
			
		rewrite.replace(expression, prefix, null);

		Logger.addUnit(fUnit.getElementName(), new LoggingUnit(expression.getStartPosition(), expression + " >>> " + prefix), Logger.CREMENT_REFACTORING);
		
		this.rewriteAST(rewrite);
		return status;
	}

	/**
	 * Rewrites the Then or Else statement if it is instanceof ExpressionStatement and an Increment/Decrement exists.
	 * New Then/Else statement is a Block.
	 * @param status
	 * @param ifNode
	 * @param crement
	 * @return
	 */
	private RefactoringStatus rewriteIfStatement(RefactoringStatus status, IfStatement ifNode, ASTNode crement) {
		AST ast = ifNode.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		
		ASTNode parent = crement.getParent();
		while(!(parent instanceof ExpressionStatement))
				parent = parent.getParent();
		ExpressionStatement expression = (ExpressionStatement)parent;
		
		if(expression.equals(ifNode.getThenStatement())) {
			Block block = this.createBlockWithExpression(ast, expression, crement);
			rewrite.replace(ifNode.getThenStatement(), block, null);
			
			Logger.addUnit(fUnit.getElementName(), new LoggingUnit(ifNode.getThenStatement().getStartPosition(), ifNode.getThenStatement() + " >>> " + block), Logger.CREMENT_REFACTORING);
		}
	
		else if((ifNode.getElseStatement() != null) && (expression.equals(ifNode.getElseStatement()))) {
			Block block = this.createBlockWithExpression(ast, expression, crement);
			rewrite.replace(ifNode.getElseStatement(), block, null);
			
			Logger.addUnit(fUnit.getElementName(), new LoggingUnit(ifNode.getElseStatement().getStartPosition(), ifNode.getElseStatement() + " >>> " + block), Logger.CREMENT_REFACTORING);
		}
		
		
		this.rewriteAST(rewrite);
		return status;
	}

	/**
	 * Replaces increment or decrement updater by variable = variable operator 1
	 * @param status
	 * @param forNode ASTNode of for loop
	 * @param crement Increment/Decrement node
	 * @return
	 */
	private RefactoringStatus rewriteForLoop(RefactoringStatus status, ForStatement forNode, ASTNode crement) {
		
		ASTNode tmp = crement;
		while(!(tmp.getParent() instanceof ForStatement))
			tmp = tmp.getParent();
		
		// Rewrite tools
		AST ast = forNode.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		
		// Increment/Decrement as Updater
		if(forNode.updaters().get(0).equals(crement)) {
			Expression expression = this.createAssignment(ast);
			ASTNode updater = (ASTNode)forNode.updaters().get(0);
			rewrite.replace(updater, expression, null);
			
			Logger.addUnit(fUnit.getElementName(), new LoggingUnit(updater.getStartPosition(), updater + " >>> " + expression), Logger.CREMENT_REFACTORING);
			
		} else if(forNode.getBody().equals(tmp)) {
			Block block = this.createBlockWithExpression(ast, (ExpressionStatement)tmp, crement);
			rewrite.replace(forNode.getBody(), this.createBlockWithExpression(ast, (ExpressionStatement)tmp, crement), null);
			
			Logger.addUnit(fUnit.getElementName(), new LoggingUnit(forNode.getBody().getStartPosition(), forNode.getBody() + " >>> " + block), Logger.CREMENT_REFACTORING);
			
		}
			
		this.rewriteAST(rewrite);
		return status;
	}
	
	/**
	 * Replaces an ExpressionStatement in a while body with a new block.
	 * Increment and decrement will be expanded.
	 * @param status
	 * @param whileNode	WhileStatementNode
	 * @param crement	Increment/Decrement Node
	 * @return
	 */
	private RefactoringStatus rewriteWhileLoop(RefactoringStatus status, WhileStatement whileNode, ASTNode crement) {
		
		ASTNode tmp = crement;
		while(!(tmp.getParent() instanceof ForStatement))
			tmp = tmp.getParent();
		
		//Rewriter
		AST ast = whileNode.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		
		if(whileNode.getBody().equals(tmp)) {
			Block block = this.createBlockWithExpression(ast, (ExpressionStatement)tmp, crement);
			rewrite.replace(whileNode.getBody(), block, null);
			
			Logger.addUnit(fUnit.getElementName(), new LoggingUnit(whileNode.getBody().getStartPosition(), whileNode.getBody() + " >>> " + block), Logger.CREMENT_REFACTORING);
		}
		
		
		this.rewriteAST(rewrite);
		return status;
	}
	
	
	
	
	//Important data
	/**
	 * True, if fVariable.parent() is of type FieldAccess.
	 * Important for further use.
	 */
	private void isFieldAccess() {
		if(fVariable.getParent() instanceof FieldAccess)
			this.isField = true;
	}
	
	/**
	 * Decides whether the Expression is Prefix or Postfix
	 */
	private void setPlacement() {
		ASTNode expression = null;
		if(isField)
			expression = fVariable.getParent().getParent();
		else
			expression = fVariable.getParent();
		
		if(expression instanceof PrefixExpression)
			placement = -1;
		else
			placement = 1;
	}
	
	/**
	 * Returns the Operator based on the old expression
	 * @param expression old expression
	 * @return operator for replacement
	 */
	private void setOperator(){
		ASTNode expression = null;
		
		if(isField)
			expression = fVariable.getParent().getParent();
		else
			expression = fVariable.getParent();
		
		if(expression instanceof PrefixExpression) {
			PrefixExpression prefix = (PrefixExpression)expression;
			
			if(prefix.getOperator().equals(PrefixExpression.Operator.INCREMENT))
				operator = Operator.PLUS;
			else
				operator = Operator.MINUS;
		} else {
			PostfixExpression postfix = (PostfixExpression)expression;
			
			if(postfix.getOperator().equals(PostfixExpression.Operator.INCREMENT))
				operator = Operator.PLUS;
			else
				operator = Operator.MINUS;
		}
	}
	
	
	
	
	//MISC
	/**
	 * Gets the last node before the BlockStatement
	 * @return ASTNode
	 */
	private ASTNode getBodyNode() {
		ASTNode bodyNode = fVariable.getParent();
		while(!(bodyNode.getParent() instanceof Block)) {
			bodyNode = bodyNode.getParent();
		}
		return bodyNode;
	}
	
	
	/**
	 * Gets the methodDeclaration block of a given ASTNode
	 * @param expression	ASTNode
	 * @return	Block
	 */
	private Block getMethodDeclaration(ASTNode expression) {
		ASTNode parent = expression.getParent();
		while(!(parent instanceof Block)) 
			parent = parent.getParent();
		return (Block)parent;
	}
	

	/**
	 * Creates a new assignment -> variable = variable (operator) 1;
	 * @param ast
	 * @return
	 */
	private Expression createAssignment(AST ast) {
		Assignment newAssignment = ast.newAssignment();
		
		//Left side
		if(isField)
			newAssignment.setLeftHandSide((FieldAccess)ASTNode.copySubtree(ast, fVariable.getParent()));
		else
			newAssignment.setLeftHandSide((SimpleName)ASTNode.copySubtree(ast, fVariable));
		
		//Right side
		InfixExpression infix = ast.newInfixExpression();
		if(isField)
			infix.setLeftOperand((FieldAccess)ASTNode.copySubtree(ast, fVariable.getParent()));
		else
			infix.setLeftOperand((SimpleName)ASTNode.copySubtree(ast, fVariable));
				
		//Right side Operator
		infix.setOperator(operator);
				
		//1
		infix.setRightOperand(ast.newNumberLiteral("1"));
				
		newAssignment.setRightHandSide(infix);	

		return newAssignment;
	}

	/**
	 * Creates a new block (for if, for, while....) made of a new ExpressionStatement without the crement,
	 * and an expanded crement
	 * @param ast	AST of if,while,for node
	 * @param expression	cremented ExpressionNode
	 * @param crement
	 * @return	new Block
	 */
	@SuppressWarnings("unchecked")
	private Block createBlockWithExpression(AST ast, ExpressionStatement expression, ASTNode crement) {
		Block newBlock = ast.newBlock();
		
		// ExpressionStatement without crement (temporarily save old ast)
		ExpressionStatement tempExpression = (ExpressionStatement)ASTNode.copySubtree(ast, expression);
		
		// i++ or --i t +i
		PrefixExpression prefix = ast.newPrefixExpression();
		prefix.setOperand((Expression)ASTNode.copySubtree(ast, fVariable));
		
		ASTNode parent = crement.getParent();
		
		if(parent instanceof Assignment) {
			Assignment assignment = (Assignment)parent;
			assignment.setRightHandSide(prefix);
			
		} else if(parent instanceof InfixExpression) {
			InfixExpression infix = (InfixExpression)parent;
			
			if (infix.getRightOperand().equals(crement))
				infix.setRightOperand(prefix);
			else if(infix.getLeftOperand().equals(crement))
				infix.setLeftOperand(prefix);
		}
		
		//Add transformed expression and crementStatement in order into the newBlock
		if(placement == -1) {
			newBlock.statements().add(ast.newExpressionStatement(this.createAssignment(ast)));
			newBlock.statements().add((ExpressionStatement)ASTNode.copySubtree(ast, expression));
		} else {
			newBlock.statements().add((ExpressionStatement)ASTNode.copySubtree(ast, expression));
			newBlock.statements().add(ast.newExpressionStatement(this.createAssignment(ast)));
		}
		
		//change ast back to normal
		expression = (ExpressionStatement)ASTNode.copySubtree(ast, tempExpression);

		return newBlock;
	}
	
}
