package com.artifice.refactoring.nodeFinder;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import com.artifice.refactoring.data.VariableJob;
import com.artifice.refactoring.engine.Refactor;
import com.artifice.refactoring.engine.RefactoredObjects;
import com.artifice.refactoring.engine.custom.ExtractDeclarationRefactoring;
import com.artifice.refactoring.log.Logger;
import com.artifice.refactoring.log.LoggingUnit;

public class VariableDeclarationFinder extends ASTVisitor{

	private ICompilationUnit unit = null;
	public void setUnit(ICompilationUnit unit) {
		this.unit = unit;
	}
	
	private boolean unitChanged = false;
	public boolean hasChanged() {
		return unitChanged;
	}
	
	private String newName = "";
	public void setNewName(String name) {
		this.newName = name;
	}
	
	private int variableCount = 0;
	public void setVariableCount(int count) {
		this.variableCount = count;
	}
	public int getVariableCount() {
		return this.variableCount;
	}
	
	@Override
	public boolean visit(VariableDeclarationFragment node) {
		if((!unitChanged)) {
			
			//Wenn gleichzeitige Initialisierung, hat Eclipse probleme--> initialisierung und deklaration vorher trennen (Vorbearbeitung
			if((node.getParent() instanceof VariableDeclarationStatement) && (node.getInitializer() != null)) {						
				
				Refactor.refactor(new ExtractDeclarationRefactoring(node, this.unit));
				this.unitChanged = true;
			}
			
			//selbiges für For schleifen
			else if ((node.getParent() instanceof VariableDeclarationExpression) && (node.getInitializer() != null)) {
				
				Refactor.refactor(new ExtractDeclarationRefactoring(node, this.unit));
				this.unitChanged = true;
			}
			
			//ansonsten kann gleich die Variable geändert werden
			else if((node.getParent() instanceof VariableDeclarationStatement)&&(!RefactoredObjects.containsVariable(this.unit, node.getName().getIdentifier()))) {
						IVariableBinding binding = node.resolveBinding();
						ILocalVariable variable = (ILocalVariable)binding.getJavaElement();
				
						RefactoredObjects.addVariable(this.unit, this.newName + "" + this.variableCount);
						this.renameVariable(new VariableJob(variable, this.newName + "" + this.variableCount++), this.unit);
					
						unitChanged = true;

				}
		}

		return super.visit(node);
	}
	
	
	@Override
	public boolean visit(SingleVariableDeclaration node) {
		if(!unitChanged) {
			if(!RefactoredObjects.containsVariable(this.unit, node.getName().getIdentifier())) {
					
				IVariableBinding binding = node.resolveBinding();
				ILocalVariable variable = (ILocalVariable)binding.getJavaElement();
			
				RefactoredObjects.addVariable(this.unit, this.newName + "" + this.variableCount);
				this.renameVariable(new VariableJob(variable, this.newName + "" + this.variableCount++), this.unit);
				
				unitChanged = true;
			}
		}
		return super.visit(node);
	}

	private void renameVariable(VariableJob variable, ICompilationUnit unit) {
		RefactoringContribution contribution = RefactoringCore.getRefactoringContribution(IJavaRefactorings.RENAME_LOCAL_VARIABLE);
		RenameJavaElementDescriptor descriptor = (RenameJavaElementDescriptor)contribution.createDescriptor();
		//set element
		descriptor.setJavaElement(variable.getlocal());		//ILocalVariable
		//set newName
		descriptor.setNewName(variable.getnewVariableName());
		descriptor.setUpdateReferences(true);
		
		System.out.println("Change variable: " + variable.getlocal().getElementName() + " -- to: " + variable.getnewVariableName() + " in" + this.unit.getElementName());
		
		try {
			RefactoringStatus status = Refactor.refactor(descriptor.createRefactoring(descriptor.validateDescriptor()));
			
			System.out.println(status);
			if(status.isOK())  {
				Logger.addUnit(unit.getElementName(), new LoggingUnit(variable.getStartPosition() , variable.getlocal().getElementName() + " >>> " + variable.getnewVariableName()), Logger.VARIABLE_REFACTORING);
				Refactor.count[0]++;
			}
			else
				Logger.addUnit(unit.getElementName(), new LoggingUnit(variable.getStartPosition() , variable.getlocal().getElementName() + " !!! Refactoring failed"), Logger.VARIABLE_REFACTORING);
			
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
	}
	
}
