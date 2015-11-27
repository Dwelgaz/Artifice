package com.artifice.refactoring.nodeFinder;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class MethodDeclarationFinder extends ASTVisitor{
	private IMethod method;
	private MethodDeclaration declaration;
	
	public void setMethod(IMethod m) {
		this.method = m;
	}
	
	public MethodDeclaration getDeclaration() {
		return this.declaration;
	}
	@Override
	public boolean visit(MethodDeclaration node) {
		IBinding binding = node.resolveBinding();
		if(binding.getJavaElement().equals(method))
			this.declaration = node;
		return super.visit(node);
	}
}
