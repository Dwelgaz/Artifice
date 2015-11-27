package com.artifice.refactoring.nodeFinder;

import java.util.LinkedList;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Assignment.Operator;

import com.artifice.refactoring.data.ContractionJob;
import com.artifice.refactoring.engine.Refactor;
import com.artifice.refactoring.engine.RefactoredObjects;
import com.artifice.refactoring.engine.custom.ContractionRefactoring;
import com.artifice.refactoring.engine.custom.ExpansionRefactoring;

public class ExpansionFinder extends ASTVisitor{
	private ICompilationUnit unit = null;
	private LinkedList<Assignment> expansionList = new LinkedList<Assignment>();
	private LinkedList<ContractionJob> contractionList = new LinkedList<ContractionJob>();
	
	public ExpansionFinder(ICompilationUnit unit) {
		super();
		this.unit = unit;
	}
	
	/**
	 * Looks for assignment that can be expanded(+=...) or
	 * shortened( a= a+ 6 --> a += 6; a = a + 1--> a++)
	 */
	@Override
	public boolean visit(Assignment assignment) {
		// EXPANSION
		if (!(assignment.getLeftHandSide() instanceof ArrayAccess)) {
			if ((assignment.getOperator().equals(Operator.PLUS_ASSIGN)) || (assignment.getOperator().equals(Operator.MINUS_ASSIGN)) ||
				(assignment.getOperator().equals(Operator.TIMES_ASSIGN)) || (assignment.getOperator().equals(Operator.DIVIDE_ASSIGN))) {
				
				if(Refactor.refactorings[3])
					this.expansionList.add(assignment);
			}
			
			if(RefactoredObjects.containsCrement(assignment.getParent().getStartPosition(), assignment))	//Assignment is result of crement expansion
				;
			//Contractions
			else {
				if (!(assignment.getRightHandSide() instanceof SimpleName)) { // e.g. this.y = y
					SimpleNameVariableFinder visitor = null;
					
					if (assignment.getLeftHandSide() instanceof SimpleName)	
						visitor = new SimpleNameVariableFinder((SimpleName)assignment.getLeftHandSide());
					
					if (assignment.getLeftHandSide() instanceof FieldAccess) { // e.g. this.field = ..
						FieldAccess field = (FieldAccess)assignment.getLeftHandSide();
						visitor = new SimpleNameVariableFinder(field.getName());
					}
					
					if (visitor != null) {
						assignment.getRightHandSide().accept(visitor);
					
						if (visitor.getVariable() != null) {
								// variable (op) variable/number or number (op) variable
							if (visitor.getVariable().getParent() instanceof InfixExpression) {
								InfixExpression infix = (InfixExpression)visitor.getVariable().getParent();
						
								boolean add = false;
								// Infixoperator = +, both sides are allowed
								if (infix.getOperator().equals(InfixExpression.Operator.PLUS))
									add = true;
								// Infixoperator = -, variable must be rightOperand
								else if (infix.getOperator().equals(InfixExpression.Operator.MINUS) && (infix.getLeftOperand().equals(visitor.getVariable())))
									add = true;
						
								if((add) && Refactor.refactorings[4])
									this.contractionList.add(new ContractionJob(assignment, visitor.getVariable()));
							}
						}
					}
				}
			}
		}
		return super.visit(assignment);
	}
	
	//HELPER
	/**
	 * Performs the refactoring of both lists.
	 * The assignments will be edited from bottom to top
	 * ---> upper assignments won't be harmed by the refactoring (startPosition)
	 */
	public void refactorLists() {
		ContractionJob contraction;
		Assignment expansion;
		
		int startExpansion = 0;
		int startContraction = 0;
		
		while((!expansionList.isEmpty()) || (!contractionList.isEmpty())) {
			
			// retrieves last objects
			expansion = expansionList.pollLast();
			contraction = contractionList.pollLast();
			
			// get offset of both elements
			startExpansion = (expansion != null) ? expansion.getStartPosition() : -1;
			startContraction = (contraction != null) ? contraction.getAssignment().getStartPosition() : -1;
			
			//the assignment with the highest start position will be changed first
			if ((startExpansion == -1) && (startContraction != -1))					//no more expansions
				this.refactorContraction(contraction);
			else if ((startExpansion != -1) && (startContraction == -1))			//no more contractions
				this.refactorExpansion(expansion);
			
			else {																	//both lists contained an item
				if(startExpansion > startContraction) {								//Expansion first
					this.refactorExpansion(expansion);
					this.contractionList.addLast(contraction);
				} else {															//Contraction first
					this.refactorContraction(contraction);
					this.expansionList.addLast(expansion);
				}
			}
		}
	}
	
	/**
	 * Refactoring for ExpansionAssignment
	 * @param assignment ExpansionAssignment
	 */
	private void refactorContraction(ContractionJob assignment) {
		System.out.println("Shorten assignment : " + assignment.getAssignment());
		ContractionRefactoring refactoring = new ContractionRefactoring(assignment, this.unit);
		Refactor.refactor(refactoring);
		
		Refactor.count[2]++;
	}
	
	/**
	 * Refactoring for ContractionAssignment
	 * @param assignment ContractionAssignment
	 */
	private void refactorExpansion(Assignment assignment) {
		System.out.println("Expand assignment : " + assignment);
		ExpansionRefactoring refactoring = new ExpansionRefactoring(assignment, this.unit);
		Refactor.refactor(refactoring);
		
		Refactor.count[1]++;
	}

}
