package com.github.firmwehr.gentle.semantic;

import com.github.firmwehr.gentle.semantic.ast.ClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LexicalScope {
	private final Map<String, ClassDeclaration> classDeclarations;
	private final Optional<ClassDeclaration> currentClass;
	private final Map<String, LocalVariableDeclaration> currentVariables;
	private final Optional<LexicalScope> parent;

	private LexicalScope(
		Map<String, ClassDeclaration> classes, Optional<ClassDeclaration> currentClass, Optional<LexicalScope> parent
	) {
		this.classDeclarations = classes;
		this.currentClass = currentClass;
		this.currentVariables = new HashMap<>();
		this.parent = parent;
	}

	public LexicalScope(Map<String, ClassDeclaration> classes, Optional<ClassDeclaration> currentClass) {
		this(classes, currentClass, Optional.empty());
	}

	public ClassDeclaration getClass(String name) throws SemanticException {
		ClassDeclaration declaration = classDeclarations.get(name);
		if (declaration == null) {
			throw new SemanticException("Class '" + name + "' not found");
		}
		return declaration;
	}

	public LexicalScope nestedScope() {
		return new LexicalScope(classDeclarations, currentClass, Optional.of(this));
	}

	public LocalVariableDeclaration getLocalVariable(String name) throws SemanticException {
		LocalVariableDeclaration declaration = currentVariables.get(name);
		if (declaration != null) {
			return declaration;
		}
		if (parent.isEmpty()) {
			throw new SemanticException("Could not find local variable " + name);
		}
		return parent.get().getLocalVariable(name);
	}

	public boolean hasLocalVariable(String name) {
		if (currentVariables.containsKey(name)) {
			return true;
		}
		return parent.map(it -> it.hasLocalVariable(name)).orElse(false);
	}

	public void addLocalVariable(LocalVariableDeclaration declaration) throws SemanticException {
		String variableName = declaration.getDeclaration()
			.orElseThrow(() -> new IllegalArgumentException("Tried to add 'this' as local variable"))
			.ident();

		if (this.currentVariables.containsKey(variableName)) {
			throw new SemanticException("Duplicate local variable '" + declaration.getDeclaration() + "'");
		}
		this.currentVariables.put(variableName, declaration);
	}

}
