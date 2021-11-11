package com.github.firmwehr.gentle.semantic.analysis;

import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperator;
import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.expression.SArrayAccessExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SBinaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SFieldAccessExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SLocalVariableExpression;
import com.github.firmwehr.gentle.source.Source;

import java.util.Optional;

/**
 * Ensures all assignment expressions assign to lvalues. In Java this is
 * <ul>
 *     <li>A variable or field access, i.e. {@code foo = 20}</li>
 *     <li>A computed member access, i.e. {@code bar.foo = 20}</li>
 *     <li>An array access, i.e. {@code foo[20] = 20}</li>
 * </ul>
 */
@SuppressWarnings("ClassCanBeRecord")
public class AssignmentLValueVisitor implements Visitor<Void> {
	private final Source source;

	public AssignmentLValueVisitor(Source source) {
		this.source = source;
	}

	@Override
	public Optional<Void> visit(SBinaryOperatorExpression binaryOperatorExpression) throws SemanticException {
		if (binaryOperatorExpression.operator() != BinaryOperator.ASSIGN) {
			return Visitor.super.visit(binaryOperatorExpression);
		}

		SExpression lhs = binaryOperatorExpression.lhs();

		boolean isVariableAccess = lhs instanceof SLocalVariableExpression;
		boolean isFieldAccess = lhs instanceof SFieldAccessExpression;
		boolean isArrayAccess = lhs instanceof SArrayAccessExpression;

		if (!(isVariableAccess || isFieldAccess || isArrayAccess)) {
			throw new SemanticException(source, binaryOperatorExpression.sourceSpan(), "invalid assignment",
				lhs.sourceSpan(), "not an lvalue");
		}

		return Visitor.super.visit(binaryOperatorExpression);
	}
}
