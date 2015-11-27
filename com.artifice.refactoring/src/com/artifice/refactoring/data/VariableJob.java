package com.artifice.refactoring.data;

import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.JavaModelException;

public class VariableJob {

	
	private int startPosition = 0;
	public int getStartPosition() {
		return this.startPosition;
	}
	
	private ILocalVariable local;
	public ILocalVariable getlocal() {
		return this.local;
	}
	public void setlocal(ILocalVariable l) {
		this.local = l;
	}

	private String newVariableName;
	public void setnewVariableName(String newName) {
		this.newVariableName = newName;
	}
	public String getnewVariableName() {
		return this.newVariableName;
	}

	public VariableJob(ILocalVariable variable, String newName)  {
		this.local = variable;
		
		try {
			this.startPosition = this.local.getSourceRange().getOffset();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		this.newVariableName = newName;
	}
	
}
