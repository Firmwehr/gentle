package com.github.firmwehr.gentle.semantic.analysis;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.expression.SLocalVariableExpression;
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
public class MainMethodLookupVisitor implements Visitor<Void> {
	private final Source source;

	private Optional<SMethod> foundMainMethod = Optional.empty();
	private Optional<LocalVariableDeclaration> mainMethodParameter = Optional.empty();

	public MainMethodLookupVisitor(Source source) {
		this.source = source;
	}

	@Override
	public Optional<Void> visit(SMethod method) throws SemanticException {
		if (!method.isStatic()) {
			return Optional.empty();
		}
		if (this.foundMainMethod.isPresent()) {
			throw new SemanticException(source, null, "Found second main method!");
		}
		if (!method.name().ident().equals("main")) {
			throw new SemanticException(source, null, "Only 'main' is allowed for static method names");
		}
		if (method.returnType().asExprType().asVoidType().isEmpty()) {
			throw new SemanticException(source, null, "The main method must have a void return type");
		}

		if (method.parameters().size() != 1) {
			throw new IllegalArgumentException("The main method must have exactly one parameter");
		}
		LocalVariableDeclaration parameter = method.parameters().get(0);

		if (parameter.getType().arrayLevel() != 1) {
			throw new SemanticException(source, null, "The main method must have a String[] parameter");
		}
		if (parameter.getType().basicType().asStringType().isEmpty()) {
			throw new SemanticException(source, null, "The main method must have a String[] parameter");
		}

		this.foundMainMethod = Optional.of(method);
		this.mainMethodParameter = Optional.of(parameter);

		return Visitor.super.visit(method);
	}

	@Override
	public Optional<Void> visit(SLocalVariableExpression localVariableExpression) throws SemanticException {
		LocalVariableDeclaration localVariable = localVariableExpression.localVariable();

		if (mainMethodParameter.map(it -> it == localVariable).orElse(false)) {
			throw new SemanticException(source, null, "Usage of main method parameter is forbidden");
		}

		return Visitor.super.visit(localVariableExpression);
	}

	public SMethod getFoundMainMethod() throws SemanticException {
		return foundMainMethod.orElseThrow(() -> new SemanticException(source, null, "Did not find a main method :/"));
	}
}
