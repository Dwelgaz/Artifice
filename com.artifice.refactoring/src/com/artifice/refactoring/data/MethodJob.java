package com.artifice.refactoring.data;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class MethodJob {

	private int startPosition = 0;
	public int getStartPosition() {
		return this.startPosition;
	}
	
	private IMethod method;
	public IMethod getMethod(){
		return method;
	}
	public void setMethod(IMethod m){
		this.method = m;
	}
	
	public IType getMethodType(){
		return method.getDeclaringType();
	}
	
	private String newMethodName;
	public String getNewMethodName(){
		return newMethodName;
	}
	public void setNewMethodName(String newName){	
		this.newMethodName = newName;
	}
	
	private boolean editable = true;
	public boolean isEditable(){
		return editable;
	}
	public void setEditable(boolean b) {
		editable = b;
	}

	public MethodJob(IMethod method, String newName)  {
		this.method = method;
		
		try {
			this.startPosition = this.method.getSourceRange().getOffset();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		this.newMethodName = newName;
	}
}
