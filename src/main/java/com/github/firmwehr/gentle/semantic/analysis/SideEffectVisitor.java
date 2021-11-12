package com.github.firmwehr.gentle.semantic.analysis;

import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperator;
import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.expression.SBinaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SMethodInvocationExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemInReadExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemOutFlushExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemOutPrintlnExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemOutWriteExpression;
import com.github.firmwehr.gentle.semantic.ast.statement.SExpressionStatement;
import com.github.firmwehr.gentle.source.Source;

import java.util.Optional;

/**
 * Ensures every {@link SExpressionStatement} has a side effect.
 */
public class SideEffectVisitor implements Visitor<Optional<Void>> {
	private final Source source;

	public SideEffectVisitor(Source source) {
		this.source = source;
	}

	@Override
	public Optional<Void> defaultReturnValue() {
		return Optional.empty();
	}

	@SuppressWarnings("DuplicateBranchesInSwitch")
	@Override
	public Optional<Void> visit(SExpressionStatement expressionStatement) throws SemanticException {
		SExpression expression = expressionStatement.expression();
		return switch (expression) {
			case SMethodInvocationExpression ignored -> Optional.empty();
			case SBinaryOperatorExpression op && op.operator() == BinaryOperator.ASSIGN -> Optional.empty();
			case SSystemInReadExpression ignored -> Optional.empty();
			case SSystemOutPrintlnExpression ignored -> Optional.empty();
			case SSystemOutWriteExpression ignored -> Optional.empty();
			case SSystemOutFlushExpression ignored -> Optional.empty();
			default -> throw new SemanticException(source, expression.sourceSpan(), "must have side effects");
		};
	}
}
