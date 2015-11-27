package com.artifice.refactoring.engine;

import java.util.HashMap;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import com.artifice.refactoring.data.DataConverter;
import com.artifice.refactoring.data.FieldJob;
import com.artifice.refactoring.data.MethodJob;
import com.artifice.refactoring.log.Logger;
import com.artifice.refactoring.log.LoggingUnit;
import com.artifice.refactoring.nodeFinder.CrementFinder;
import com.artifice.refactoring.nodeFinder.ExpansionFinder;
import com.artifice.refactoring.nodeFinder.IfStatementFinder;
import com.artifice.refactoring.nodeFinder.LoopFinder;
import com.artifice.refactoring.nodeFinder.VariableDeclarationFinder;

public class Refactor {

		public static HashMap<ICompilationUnit, RefactoringLists> refactoringMap = new HashMap<ICompilationUnit, RefactoringLists>();

		public static int[] count = new int[5]; //Renaming, Expansion, Contraction, Loop, Conditional
		
		public static boolean[] refactorings = new boolean[7]; // 0 meth, 1 field, 2 var, 3 exp, 4 contr, 5 loop, 6 condt
		
		/**
		 * Clears methods, fields and variables
		 */
		public static void clearLists() {
			refactoringMap.clear();
			RefactoredObjects.clearSet();
			
			refactorings[0] = false;
			refactorings[1] = false;
			refactorings[2] = false;
			refactorings[3] = false;
			refactorings[4] = false;
			refactorings[5] = false;
			refactorings[6] = false;
		}
		
		public static void addCompilationUnit(ICompilationUnit unit) {
			if(!refactoringMap.containsKey(unit))
				refactoringMap.put(unit, new RefactoringLists());
		}
		
		//RENAMING
		//methods
		/**
		 * Performs refactoring for methods
		 */
		public static void renameMethods() {
			for (ICompilationUnit unit : refactoringMap.keySet())
				for(MethodJob m : refactoringMap.get(unit).getMethods())
					if (m.isEditable())
						renameMethod(m, unit);
		}
		
		private static void renameMethod(MethodJob method, ICompilationUnit unit) {
			RefactoringContribution contribution = RefactoringCore.getRefactoringContribution(IJavaRefactorings.RENAME_METHOD);
			RenameJavaElementDescriptor descriptor = (RenameJavaElementDescriptor)contribution.createDescriptor();
			
			//set element
			descriptor.setJavaElement(method.getMethod());
			//set newName
			descriptor.setNewName(method.getNewMethodName());
			descriptor.setUpdateReferences(true);
			System.out.println("Change method: " + method.getMethod().getElementName() + " -- to: " + method.getNewMethodName());

			try {
				RefactoringStatus status = refactor(descriptor.createRefactoring(descriptor.validateDescriptor()));
				
				if(status.isOK()) {
					Logger.addUnit(unit.getElementName(), new LoggingUnit(method.getStartPosition() , method.getMethod().getElementName() + " >>> " + method.getNewMethodName()), Logger.METHOD_REFACTORING);
					count[0]++;
				}
				else
					Logger.addUnit(unit.getElementName(), new LoggingUnit(method.getStartPosition() , method.getMethod().getElementName() + " !!! Refactoring failed"), Logger.METHOD_REFACTORING);
				
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		
		// fields
		/**
		 * Performs refactoring for fields
		 */
		public static void renameFields() {
			for(ICompilationUnit unit : refactoringMap.keySet())
				for(FieldJob f : refactoringMap.get(unit).getFields())
					if(f.getEditable())
						renameField(f, unit);
		}
		
		private static void renameField(FieldJob field, ICompilationUnit unit) {
			RefactoringContribution contribution = RefactoringCore.getRefactoringContribution(IJavaRefactorings.RENAME_FIELD);
			RenameJavaElementDescriptor descriptor = (RenameJavaElementDescriptor)contribution.createDescriptor();
			
			//set element
			descriptor.setJavaElement(field.getField());
			//set newName
			descriptor.setNewName(field.getnewFieldName());
			descriptor.setUpdateReferences(true);
				
			System.out.println("Change field: " + field.getField().getElementName() + " -- to: " + field.getnewFieldName());

			try {
				RefactoringStatus status = refactor(descriptor.createRefactoring(descriptor.validateDescriptor()));
				
				if(status.isOK())  {
					Logger.addUnit(unit.getElementName(), new LoggingUnit(field.getStartPosition() , field.getField().getElementName() + " >>> " + field.getnewFieldName()), Logger.VARIABLE_REFACTORING);
					count[0]++;
				}
				else
					Logger.addUnit(unit.getElementName(), new LoggingUnit(field.getStartPosition() , field.getField().getElementName() + " !!! Refactoring failed"), Logger.VARIABLE_REFACTORING);
				
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		
		// variables
		/**
		 * Performs refactoring for variables
		 */
		public static void renameVariables(){
			for(ICompilationUnit unit : refactoringMap.keySet()) {
				
				boolean unitChanged = true;
				int i = 0;
				String newName = DataConverter.newNames[2];
				
				while(unitChanged) {
					VariableDeclarationFinder variableVisitor = new VariableDeclarationFinder();
					variableVisitor.setNewName(newName);
					variableVisitor.setVariableCount(i);
					variableVisitor.setUnit(unit);
					
					getCompilationUnit(unit).accept(variableVisitor);
					
					unitChanged = variableVisitor.hasChanged();
					i = variableVisitor.getVariableCount();
				}
			}
			
		}
		
		//REFACTORING
		/**
		 * Main Refactoring method
		 * @param descriptor
		 */
		public static RefactoringStatus refactor(Refactoring refactoring) {
			try {
				RefactoringStatus status = new RefactoringStatus();
				//create refactoring
				PerformRefactoringOperation refactoringOp = new PerformRefactoringOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
				
				//perform action on workspace
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				workspace.run(refactoringOp, new NullProgressMonitor());
				
				status.merge(refactoringOp.getValidationStatus());
				status.merge(refactoringOp.getConditionStatus());
				
				return status;
			} catch (CoreException e){
				e.printStackTrace();
				return RefactoringStatus.createErrorStatus("Refactoring failed!");
			}
		}

		/**
		 * Anchorpoint to perform custom Refactorings
		 */
		public static void refactorCustoms() {
			
			for(ICompilationUnit unit : refactoringMap.keySet()) {
				boolean unitChanged = true;
				
				//EXPANSION - CONTRACTION
				if (refactorings[3] || refactorings[4]) {
					ExpansionFinder assignmentVisitor = new ExpansionFinder(unit);
					getCompilationUnit(unit).accept(assignmentVisitor);
					assignmentVisitor.refactorLists();
				}
				
				//INCREMENT - DECREMENT
				if (refactorings[3]) {
				unitChanged = true;
					while(unitChanged) {
						CrementFinder crementVisitor = new CrementFinder();
						getCompilationUnit(unit).accept(crementVisitor);
						unitChanged = crementVisitor.hasChanged();
					}
				}
				
				//IFSTATEMENT
				if (refactorings[6]) {
					IfStatementFinder ifVisitor = new IfStatementFinder();
					getCompilationUnit(unit).accept(ifVisitor);
					ifVisitor.refactorLists();
				}
				
				//LOOPS
				if (refactorings[5]) {
					unitChanged = true;
					while(unitChanged) {
						LoopFinder loopVisitor = new LoopFinder(unit);
						getCompilationUnit(unit).accept(loopVisitor);
						unitChanged = loopVisitor.hasChanged();
					}
				}
				
			}
			
		}
		
		/**
		 * Creates the AST based on the ICompilationUnit
		 * @return CompilationUnit AST
		 */
		private static CompilationUnit getCompilationUnit(ICompilationUnit unit) {
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(unit);
			parser.setResolveBindings(true);
			return (CompilationUnit)parser.createAST(null);
		}
		
}
