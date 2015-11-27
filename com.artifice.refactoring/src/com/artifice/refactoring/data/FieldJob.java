package com.artifice.refactoring.data;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;


public class FieldJob {

	private int startPosition = 0;
	public int getStartPosition() {
		return this.startPosition;
	}
	
	private IField field;
	public IField getField() {
		return this.field;
	}
	public void setField(IField f) {
		this.field = f;
	}
	
	public IType getDeclaringType() {
		return field.getDeclaringType();
	}

	private String newFieldName;
	public void setnewFieldName(String newName) {
		this.newFieldName = newName;
	}
	public String getnewFieldName() {
		return this.newFieldName;
	}

	private boolean editable = true;
	public boolean getEditable(){
		return editable;
	}
	public void setEditable(boolean b) {
		editable = b;
	}

	public FieldJob(IField field, String newName)  {
		this.field = field;
		
		try {
			this.startPosition = this.field.getSourceRange().getOffset();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		this.newFieldName = newName;
	}

}
