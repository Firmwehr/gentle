package com.github.firmwehr.gentle.semantic.analysis;

import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperator;
import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.expression.SBinaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SMethodInvocationExpression;
import com.github.firmwehr.gentle.semantic.ast.statement.SExpressionStatement;
import com.github.firmwehr.gentle.source.Source;

import java.util.Optional;

/**
 * Ensures every {@link SExpressionStatement} has a side effect.
 */
@SuppressWarnings("ClassCanBeRecord")
public class SideEffectVisitor implements Visitor<Void> {
	private final Source source;

	public SideEffectVisitor(Source source) {
		this.source = source;
	}

	@SuppressWarnings("DuplicateBranchesInSwitch")
	@Override
	public Optional<Void> visit(SExpressionStatement expressionStatement) throws SemanticException {
		SExpression expression = expressionStatement.expression();
		return switch (expression) {
			case SMethodInvocationExpression ignored -> Optional.empty();
			case SBinaryOperatorExpression op && op.operator() == BinaryOperator.ASSIGN -> Optional.empty();
			default -> throw new SemanticException(source, null, "Expression statement must habe side " + "effects");
		};
	}
}
