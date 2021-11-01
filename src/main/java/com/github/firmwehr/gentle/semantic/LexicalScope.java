package com.github.firmwehr.gentle.semantic;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.semantic.ast.ClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.source.Source;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LexicalScope {
	private final Source source;
	private final Map<String, ClassDeclaration> classDeclarations;
	private final Optional<ClassDeclaration> currentClass;
	private final Map<String, LocalVariableDeclaration> currentVariables;
	private final Optional<LexicalScope> parent;

	private LexicalScope(
		Source source,
		Map<String, ClassDeclaration> classes,
		Optional<ClassDeclaration> currentClass,
		Optional<LexicalScope> parent
	) {
		this.source = source;
		this.classDeclarations = classes;
		this.currentClass = currentClass;
		this.currentVariables = new HashMap<>();
		this.parent = parent;
	}

	public LexicalScope(
		Source source, Map<String, ClassDeclaration> classes, Optional<ClassDeclaration> currentClass
	) {
		this(source, classes, currentClass, Optional.empty());
	}

	public ClassDeclaration getClass(Ident name) throws SemanticException {
		ClassDeclaration declaration = classDeclarations.get(name.ident());
		if (declaration == null) {
			throw new SemanticException(source, name.sourceSpan(), "class not found");
		}
		return declaration;
	}

	public LexicalScope nestedScope() {
		return new LexicalScope(source, classDeclarations, currentClass, Optional.of(this));
	}

	public LocalVariableDeclaration getLocalVariable(Ident name) throws SemanticException {
		LocalVariableDeclaration declaration = currentVariables.get(name.ident());
		if (declaration != null) {
			return declaration;
		}
		if (parent.isEmpty()) {
			throw new SemanticException(source, name.sourceSpan(), "local variable not found");
		}
		return parent.get().getLocalVariable(name);
	}

	public boolean hasLocalVariable(String name) {
		if (currentVariables.containsKey(name)) {
			return true;
		}
		return parent.map(it -> it.hasLocalVariable(name)).orElse(false);
	}

	public void addLocalVariable(LocalVariableDeclaration localVarDecl) throws SemanticException {
		Ident localVarIdent = localVarDecl.getDeclaration()
			.orElseThrow(() -> new IllegalArgumentException("adding 'this' declaration to local variables"));

		Optional<LocalVariableDeclaration> currentVarDecl =
			Optional.ofNullable(currentVariables.get(localVarIdent.ident()));

		if (currentVarDecl.isPresent()) {
			Ident currentVarIdent = currentVarDecl.get()
				.getDeclaration()
				.orElseThrow(() -> new IllegalStateException("'this' declaration in local variables"));

			throw new SemanticException(source, localVarIdent.sourceSpan(), "duplicate variable name",
				currentVarIdent.sourceSpan(), "already declared here");
		}

		currentVariables.put(localVarIdent.ident(), localVarDecl);
	}

}
