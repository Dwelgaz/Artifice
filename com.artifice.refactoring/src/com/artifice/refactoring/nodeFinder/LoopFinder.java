package com.artifice.refactoring.nodeFinder;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import com.artifice.refactoring.engine.Refactor;
import com.artifice.refactoring.engine.RefactoredObjects;
import com.artifice.refactoring.engine.custom.LoopRefactoring;

public class LoopFinder extends ASTVisitor{
	private boolean unitChanged = false;
	private ICompilationUnit fUnit = null;
	
	public boolean hasChanged() {
		return unitChanged;
	}
	
	public LoopFinder(ICompilationUnit unit) {
		super();
		this.fUnit = unit;
	}
	
	@Override
	public boolean visit(WhileStatement node) {
		if(!unitChanged) {
			if(!RefactoredObjects.containsLoop(fUnit, node)) {
				System.out.println("Transform WhileStatement: " + node);
				LoopRefactoring refactoring = new LoopRefactoring(node, fUnit);
				Refactor.refactor(refactoring);
				unitChanged = true;
				
				Refactor.count[3]++;
			}
		}
		return super.visit(node);
	}
	
	@Override
	public boolean visit(ForStatement node) {
		if(!unitChanged) {
			
			if(!RefactoredObjects.containsLoop(fUnit, node)) {
				System.out.println("Transform ForStatement: " + node);
				LoopRefactoring refactoring = new LoopRefactoring(node, fUnit);
				Refactor.refactor(refactoring);
				unitChanged = true;
				
				Refactor.count[3]++;
			}
		}
		return super.visit(node);
	}

}
