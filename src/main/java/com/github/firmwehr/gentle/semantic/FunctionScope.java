package com.github.firmwehr.gentle.semantic;

import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.source.Source;

import java.util.Optional;

public record FunctionScope(
	Source source,
	Namespace<SClassDeclaration> classes,
	Optional<SClassDeclaration> currentClass,
	StackedNamespace<LocalVariableDeclaration> localVariables
) {
}
