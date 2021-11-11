package com.github.firmwehr.gentle.semantic.analysis;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.expression.SArrayAccessExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SBinaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SMethodInvocationExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SNewArrayExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemOutPrintlnExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemOutWriteExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SUnaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.statement.SIfStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SReturnStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SWhileStatement;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidType;
import com.github.firmwehr.gentle.source.Source;

import java.util.List;
import java.util.Optional;

/**
 * Verifies the program typechecks by ensuring:
 * <ul>
 *     <li>If and while conditions are booleans</li>
 *     <li>Array access indices are integers</li>
 *     <li>Array sizes are integers</li>
 *     <li>Unary operators operate on correct types</li>
 *     <li>The rhs of an assignment is assignable to the lhs</li>
 *     <li>Either the lhs is assignable to the rhs or vice versa for equality comparisons</li>
 *     <li>Binary operators operate on correct types</li>
 *     <li>System out/println receive integers</li>
 *     <li>There are as many method call arguments as parameters and the argument types are assignable to the
 *     parameter types</li>
 *     <li>Void methods return nothing</li>
 *     <li>The returned value is assignable to the return type</li>
 * </ul>
 */
public class TypecheckVisitor implements Visitor<Void> {
	private final Source source;

	private SMethod currentMethod;

	public TypecheckVisitor(Source source) {
		this.source = source;
	}

	@Override
	public Optional<Void> visit(SMethod method) throws SemanticException {
		this.currentMethod = method;
		return Visitor.super.visit(method);
	}

	@Override
	public Optional<Void> visit(SIfStatement ifStatement) throws SemanticException {
		assertIsBoolean(ifStatement.condition());

		return Visitor.super.visit(ifStatement);
	}

	@Override
	public Optional<Void> visit(SWhileStatement whileStatement) throws SemanticException {
		assertIsBoolean(whileStatement.condition());

		return Visitor.super.visit(whileStatement);
	}

	@Override
	public Optional<Void> visit(SArrayAccessExpression arrayExpression) throws SemanticException {
		SExpression expression = arrayExpression.expression();

		Optional<SNormalType> hasType = expression.type().asNormalType();
		SNormalType expectedType = arrayExpression.type();

		if (hasType.isEmpty() || hasType.get().arrayLevel() == 0 ||
			hasType.get().arrayLevel() != expectedType.arrayLevel() + 1 ||
			!hasType.get().basicType().equals(expectedType.basicType())) {

			throw new SemanticException(source, arrayExpression.sourceSpan(), "invalid array access",
				expression.sourceSpan(),
				"has type " + expression.type().format() + ", expected type " + expectedType.format());
		}

		assertIsInt(arrayExpression.index());

		return Visitor.super.visit(arrayExpression);
	}

	@Override
	public Optional<Void> visit(SNewArrayExpression newArrayExpression) throws SemanticException {
		// This shouldn't happen since the parser should never produce a NewArrayExpression with array level 0, but
		// better check again to be sure
		if (newArrayExpression.type().arrayLevel() == 0) {
			throw new IllegalArgumentException("Creating non-array array");
		}

		assertIsInt(newArrayExpression.size());

		return Visitor.super.visit(newArrayExpression);
	}

	@Override
	public Optional<Void> visit(SUnaryOperatorExpression unaryOperatorExpression) throws SemanticException {
		switch (unaryOperatorExpression.operator()) {
			case LOGICAL_NOT -> assertIsBoolean(unaryOperatorExpression.expression());
			case NEGATION -> assertIsInt(unaryOperatorExpression.expression());
		}

		return Visitor.super.visit(unaryOperatorExpression);
	}

	@Override
	public Optional<Void> visit(SBinaryOperatorExpression binaryOperatorExpression) throws SemanticException {
		switch (binaryOperatorExpression.operator()) {
			case ASSIGN -> {
				SExprType rhsType = binaryOperatorExpression.rhs().type();
				SExprType lhsType = binaryOperatorExpression.lhs().type();

				if (!rhsType.isAssignableTo(lhsType)) {
					throw new SemanticException(source, binaryOperatorExpression.sourceSpan(),
						"invalid assignment, incompatible types", binaryOperatorExpression.lhs().sourceSpan(),
						"has type " + lhsType.format(), binaryOperatorExpression.rhs().sourceSpan(),
						"has type " + rhsType.format());
				}
			}
			case EQUAL, NOT_EQUAL -> {
				SExprType rhsType = binaryOperatorExpression.rhs().type();
				SExprType lhsType = binaryOperatorExpression.lhs().type();

				if (!lhsType.isAssignableTo(rhsType) && !rhsType.isAssignableTo(lhsType)) {
					throw new SemanticException(source, binaryOperatorExpression.sourceSpan(),
						"invalid comparison, incompatible types", binaryOperatorExpression.lhs().sourceSpan(),
						"has type " + lhsType.format(), binaryOperatorExpression.rhs().sourceSpan(),
						"has type " + rhsType.format());
				}
			}
			case LOGICAL_OR, LOGICAL_AND -> {
				assertIsBoolean(binaryOperatorExpression.lhs());
				assertIsBoolean(binaryOperatorExpression.rhs());
			}
			case LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL, ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO -> {
				assertIsInt(binaryOperatorExpression.lhs());
				assertIsInt(binaryOperatorExpression.rhs());
			}
		}

		return Visitor.super.visit(binaryOperatorExpression);
	}

	@Override
	public Optional<Void> visit(SSystemOutWriteExpression systemOutWriteExpression) throws SemanticException {
		assertIsInt(systemOutWriteExpression.argument());

		return Visitor.super.visit(systemOutWriteExpression);
	}

	@Override
	public Optional<Void> visit(SSystemOutPrintlnExpression systemOutPrintlnExpression) throws SemanticException {
		assertIsInt(systemOutPrintlnExpression.argument());

		return Visitor.super.visit(systemOutPrintlnExpression);
	}

	@Override
	public Optional<Void> visit(SMethodInvocationExpression methodInvocationExpression) throws SemanticException {
		SMethod target = methodInvocationExpression.method();
		List<LocalVariableDeclaration> parameters = target.parameters();
		List<SExpression> arguments = methodInvocationExpression.arguments();

		if (arguments.size() < parameters.size()) {
			throw new SemanticException(source, methodInvocationExpression.postfixSpan(), "too few arguments",
				target.name().sourceSpan(), "has " + parameters.size() + " parameters");
		}
		if (arguments.size() > parameters.size()) {
			throw new SemanticException(source, methodInvocationExpression.postfixSpan(), "too many arguments",
				target.name().sourceSpan(), "has " + parameters.size() + " parameters");
		}

		for (int i = 0; i < parameters.size(); i++) {
			SExpression argument = arguments.get(i);
			LocalVariableDeclaration parameter = parameters.get(i);

			if (!argument.type().isAssignableTo(parameter.getType())) {
				throw new SemanticException(source, methodInvocationExpression.postfixSpan(),
					"invalid call, incompatible types", argument.sourceSpan(), "has type " + argument.type().format(),
					parameter.getTypeSpan(), "expected this type");
			}
		}

		return Visitor.super.visit(methodInvocationExpression);
	}

	@Override
	public Optional<Void> visit(SReturnStatement returnStatement) throws SemanticException {
		Optional<SExpression> returnStatementValue = returnStatement.returnValue();

		if (currentMethod.returnType() instanceof SVoidType) {
			if (returnStatementValue.isPresent()) {
				throw new SemanticException(source, returnStatementValue.get().sourceSpan(), "cannot return a value",
					currentMethod.returnTypeSpan(), "method returns nothing");
			}
			return Visitor.super.visit(returnStatement);
		}

		if (returnStatementValue.isEmpty()) {
			throw new SemanticException(source, returnStatement.sourceSpan(), "must return a value",
				currentMethod.returnTypeSpan(), "method returns something");
		}

		if (!returnStatementValue.get().type().isAssignableTo(currentMethod.returnType().asExprType())) {
			throw new SemanticException(source, returnStatement.sourceSpan(), "invalid return, incompatible types",
				returnStatementValue.get().sourceSpan(), "has type " + returnStatementValue.get().type().format(),
				currentMethod.returnTypeSpan(),
				"method returns type " + currentMethod.returnType().asExprType().format());
		}

		return Visitor.super.visit(returnStatement);
	}

	private void assertIsBoolean(SExpression expression) throws SemanticException {
		if (expression.type().asBooleanType().isEmpty()) {
			throw new SemanticException(source, expression.sourceSpan(), "expected type boolean");
		}
	}

	private void assertIsInt(SExpression expression) throws SemanticException {
		if (expression.type().asIntType().isEmpty()) {
			throw new SemanticException(source, expression.sourceSpan(), "expected type int");
		}
	}

}
