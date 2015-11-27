package com.artifice.refactoring.data;

import java.util.Arrays;
import java.util.LinkedList;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import com.artifice.refactoring.engine.Refactor;

/**
 * The DataConverter class is used, to convert different IJavaElements, into
 * specific jobs (found in data). The jobs will be used during the refactoring process.
 * @author Daniel Meyer, 2012
 *
 */
public class DataConverter {
	
	public static String[] newNames = {"m", "f", "v"}; // 1 meth, 2 field, 3 var
	
	public static void reset() {
		newNames[0] = "m";
		newNames[1] = "f";
		newNames[2] = "v";
	}
	
	// Main
	public static void getData() {
		
		int classCounter = 0;
		for(ICompilationUnit unit : Refactor.refactoringMap.keySet()) {
			getData(unit, classCounter);
			classCounter++;
		}
	}
	
		/**
		 * Searches through all TypeDeclarations of the selected .java file,
		 * During the process every method(without constructor, and overriden methods)
		 * , field and variable declaration will be converted into their specific
		 * refactoring job.
		 * The results are safed in the static class Refactor.
		 */
	private static void getData(ICompilationUnit unit, int classCounter) {
		LinkedList<IMethod> methodList = new LinkedList<IMethod>();
		LinkedList<IField> fieldList = new LinkedList<IField>();
			
		try {
				
			//Main-classes
			for(IType type : unit.getTypes()) {
				methodList.addAll(Arrays.asList(type.getMethods()));
				fieldList.addAll(Arrays.asList(type.getFields()));
					
				//Subclasses
				IType[] subTypes = type.getTypes();
				for(IType t : subTypes) {
					methodList.addAll(Arrays.asList(t.getMethods()));
					fieldList.addAll(Arrays.asList(t.getFields()));
				}
			}
				
			//Perform data conversion
			if ((Refactor.refactorings[0]) || (Refactor.refactorings[2]))
				createMethodJobs(methodList, unit, classCounter);
			if (Refactor.refactorings[1])
				createFieldJobs(fieldList, unit, classCounter);
//			if (Refactor.refactorings[2])
//				createDataFromMethods(methodList, unit);
			
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}
	
	
	
	// TypeDeclaration data
	/** 
	 * Converts the given IMethods into MethodJobs.
	 * The jobs are saved in Refactor.methods.
	 * @param methodList LinkedList containing IMethod objects
	 */
	private static void createMethodJobs(LinkedList<IMethod> methodList, ICompilationUnit unit, int classCounter) {
		
		IType currentType = null;
		int i = 0;
		for(IMethod m : methodList) {
			
			//Each new DeclaringType sets counter to 0
			if(currentType == null)
				currentType = m.getDeclaringType();
			else if(currentType != m.getDeclaringType()) {
				currentType = m.getDeclaringType();
				i = 0;
			}
			try {
				if((!hasSuperMethod(m)) && (!m.isConstructor()) && (!m.isMainMethod()))
					Refactor.refactoringMap.get(unit).getMethods().add(new MethodJob(m, newNames[0] + "" +i++ + "" + classCounter));
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Converts the given IFields into FieldJobs.
	 * The jobs are saved in Refactor.fields.
	 * @param fieldList LinkedList containing IField objects
	 */
	private static void createFieldJobs(LinkedList<IField> fieldList, ICompilationUnit unit, int classCounter) {
		int i = 0;
		IType currentType = null;
		
		for(IField f : fieldList)
		{
			// if new declaringType, naming can start over at 0
			if ((currentType == null) || (currentType != f.getDeclaringType())) {
				i = 0;
				currentType = f.getDeclaringType();
			}
			Refactor.refactoringMap.get(unit).getFields().add(new FieldJob(f, newNames[1] + "" +i++ + "" + classCounter));
		}
	}
	
	
	
	
	// Inner method data
	/**
	 * Gets refactorable data from method's body
	 * @param methodList List containing IMethods
	 */
//	private static void createDataFromMethods(LinkedList<IMethod> methodList, ICompilationUnit unit) {
//		if (methodList.isEmpty())
//			return ;
//		
//		for (IMethod method : methodList) {
//			int counter = 0;
//			// Parameters
//			counter = createJobsFromParameters(method, counter, unit);
//			 
//			// get MethodDeclaration Node
//			MethodDeclaration declaration = getMethodDeclarationNode(method, unit);
//			 
//			// VariableDeclarations in Body
//			counter = createJobsFromVariableDeclarations(declaration, method, counter, unit);
//		}
//	}
	
//	/** 
//	 * Converts all variable declaration inside a method into VariableJobs.
//	 * The Jobs are saved in Refactor.variables
//	 */
//	private static int createJobsFromParameters(IMethod method, int counter, ICompilationUnit unit) {
//		try {
//			//Parameters
//			ILocalVariable[] parameters = method.getParameters();
//				
//			for(ILocalVariable variable : parameters)
//				Refactor.refactoringMap.get(unit).getVariables().add(new VariableJob(variable, method, newNames[2] + "" +counter++));
//			return counter;
//		} catch (JavaModelException e) {
//			e.printStackTrace();
//		}
//		return counter;
//	}
	
//	/**
//	 * Searches for VariableDeclarationFragments inside a MethodDeclaration,
//	 * and adds them to the VariableJob list.
//	 * @param method Method, which will be searched
//	 * @param i counter, used for the new variable name
//	 */
//	private static int createJobsFromVariableDeclarations(MethodDeclaration declaration, IMethod method, int counter, ICompilationUnit unit) {
//		// Search for all VariableDeclarationFragments
//		VariableDeclarationFinder visitor = new VariableDeclarationFinder();
//		declaration.accept(visitor);
//			
//		// Resolve bindings, get IJavaElement, and create new job
//		for (VariableDeclarationFragment fragment : visitor.getFragments()) {
//			IVariableBinding binding = fragment.resolveBinding();
//			
//		ILocalVariable variable = (ILocalVariable)binding.getJavaElement();
//			Refactor.refactoringMap.get(unit).getVariables().add(new VariableJob(variable, method, newNames[2] + "" + counter++));
//		}
//		return counter;
//	}	
	
	
	
	// helper
//	/**
//	 * Gets the ASTNode from the given IMethod
//	 * @param method
//	 * @return MethodDeclaration Node
//	 */
//	private static MethodDeclaration getMethodDeclarationNode(IMethod method, ICompilationUnit unit) {
//
//		ASTParser parser = ASTParser.newParser(AST.JLS3);
//		parser.setKind(ASTParser.K_COMPILATION_UNIT);
//		parser.setSource(unit);
//		parser.setResolveBindings(true);
//		CompilationUnit cUnit = (CompilationUnit)parser.createAST(null);
//
//		MethodDeclarationFinder visitor = new MethodDeclarationFinder();
//		visitor.setMethod(method);
//			
//		cUnit.accept(visitor);
//		MethodDeclaration declaration = visitor.getDeclaration();
//			
//		return declaration;
//	}

	/**
	 * Checks if a method has the annotation "@Override"
	 * @param method 
	 * @return	true, if method overrides supermethod 
	 */
	private static boolean hasSuperMethod(IMethod method) {
		try {
			IAnnotation[] annotations= method.getAnnotations();
			for(IAnnotation anon : annotations) {
				if(anon.getElementName().equals("Override"))
						return true;
				else
					return false;
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return false;
	}
}

