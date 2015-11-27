package com.artifice.refactoring.popup.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.artifice.refactoring.UI.Main;
import com.artifice.refactoring.engine.Refactor;
import com.artifice.refactoring.log.Logger;


public class Rename implements IObjectActionDelegate {

	private Shell shell;
	
	//main methods
	/**
	 * Constructor for Action1.
	 */
	public Rename() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action){
		Logger.initialize();
		
		Main window = new Main(shell);
		int decision = window.open();
		
		if(decision == 0) {
			Logger.createLogFile();
			
			Refactor.clearLists();
			Logger.clearAll();
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		Refactor.clearLists();
		Logger.clearAll();
		
		if(selection instanceof IStructuredSelection){
			//Interface selection
			IStructuredSelection extended = (IStructuredSelection) selection;
			Object[] elements = extended.toArray();
			
			// Selection must be of type ICompilationUnit
			if (elements.length == 1 && elements[0] instanceof IProject) {
				
				IJavaProject project = JavaCore.create((IProject) elements[0]);
				Logger.setProjectName(project.getElementName());
				this.getCompilationUnits(project);
				
				action.setEnabled(true);
				
			}
			else
				action.setEnabled(false);
		}	
	}
	
	/**
	 * Adds all ICompilationUnits inside a Project to the RefactoringMap
	 * @param project
	 */
	public void getCompilationUnits(IJavaProject project) {
		try {
			for(IPackageFragment fragment : project.getPackageFragments())
				for(ICompilationUnit unit : fragment.getCompilationUnits())
					Refactor.addCompilationUnit(unit);
			
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}
	
	
	
}
