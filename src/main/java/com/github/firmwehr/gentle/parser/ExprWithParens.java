package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.source.SourceSpan;

/**
 * An {@link Expression} that may or may not be surrounded by parentheses.
 */
public record ExprWithParens(
	Expression expression,
	SourceSpan parenSourceSpan
) {
	public ExprWithParens(Expression expression) {
		this(expression, expression.sourceSpan());
	}
}
