package com.github.firmwehr.gentle.semantic.analysis;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.expression.SLocalVariableExpression;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidType;
import com.github.firmwehr.gentle.source.Source;

import java.util.Optional;

/**
 * Finds the main method, ensures it is globally unique and exists and:
 * <ul>
 *     <li>Is named main</li>
 *     <li>Has a single parameter of type String[]</li>
 *     <li>Has a void return type</li>
 *     <li>Is static</li>
 *     <li>The parameter is never used in the method</li>
 * </ul>
 */
public class MainMethodLookupVisitor implements Visitor<Optional<Void>> {
	private final Source source;

	private Optional<SMethod> foundMainMethod = Optional.empty();
	private Optional<LocalVariableDeclaration> mainMethodParameter = Optional.empty();

	public MainMethodLookupVisitor(Source source) {
		this.source = source;
	}

	@Override
	public Optional<Void> defaultReturnValue() {
		return Optional.empty();
	}

	@Override
	public Optional<Void> visit(SMethod method) throws SemanticException {
		if (!method.isStatic()) {
			return Optional.empty();
		}
		if (this.foundMainMethod.isPresent()) {
			throw new SemanticException(source, method.name().sourceSpan(), "duplicate main method",
				foundMainMethod.get().name().sourceSpan(), "already defined here");
		}
		if (!method.name().ident().equals("main")) {
			throw new SemanticException(source, method.name().sourceSpan(), "main method must be named 'main'");
		}

		if (!(method.returnType() instanceof SVoidType)) {
			throw new IllegalArgumentException("The main method must have a void return type");
		}
		if (method.parameters().size() != 1) {
			throw new IllegalArgumentException("The main method must have exactly one parameter");
		}

		LocalVariableDeclaration parameter = method.parameters().get(0);

		if (parameter.type().arrayLevel() != 1 || parameter.type().basicType().asStringType().isEmpty()) {
			throw new SemanticException(source, parameter.typeSpan(), "expected String[]");
		}

		this.foundMainMethod = Optional.of(method);
		this.mainMethodParameter = Optional.of(parameter);

		return Visitor.super.visit(method);
	}

	@Override
	public Optional<Void> visit(SLocalVariableExpression localVariableExpression) throws SemanticException {
		LocalVariableDeclaration localVariable = localVariableExpression.localVariable();

		if (mainMethodParameter.map(it -> it == localVariable).orElse(false)) {
			throw new SemanticException(source, localVariableExpression.sourceSpan(),
				"illegal use of main method parameter", localVariable.declaration().sourceSpan(), "declared here");
		}

		return Visitor.super.visit(localVariableExpression);
	}

	public SMethod getFoundMainMethod() throws SemanticException {
		return foundMainMethod.orElseThrow(() -> new SemanticException(source, "no main method found"));
	}
}
