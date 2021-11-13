package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperator;
import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.basictype.SBooleanType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SIntType;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.source.SourceSpan;

public record SBinaryOperatorExpression(
	SExpression lhs,
	SExpression rhs,
	BinaryOperator operator,
	SourceSpan sourceSpan
) implements SExpression {
	@Override
	public SExprType type() {
		return switch (operator) {
			case ASSIGN -> lhs.type();
			case LOGICAL_OR, LOGICAL_AND, EQUAL, NOT_EQUAL, LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> new SNormalType(
				new SBooleanType());
			case ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO -> new SNormalType(new SIntType());
		};
	}

	@Override
	public <T> T accept(Visitor<T> visitor) throws SemanticException {
		return visitor.visit(this);
	}
}
