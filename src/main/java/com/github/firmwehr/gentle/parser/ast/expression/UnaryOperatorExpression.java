package com.github.firmwehr.gentle.parser.ast.expression;


import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.SourceSpan;

public record UnaryOperatorExpression(
	UnaryOperator operator,
	Expression expression,
	SourceSpan sourceSpan
) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p, Parentheses parens) {
		if (parens == Parentheses.INCLUDE) {
			p.add("(");
		}

		p.add(operator.getName());

		if (operator == UnaryOperator.NEGATION && expression instanceof IntegerLiteralExpression) {
			// Positive integer literals are simple enough that they usually don't need parentheses, even in a
			// parenthesized context (as the example in Blatt 4 demonstrates). Sadly, this goes wrong with negations
			// and thus, we make an exception here.
			p.add("(").add(expression, Parentheses.OMIT).add(")");
		} else {
			p.add(expression);
		}

		if (parens == Parentheses.INCLUDE) {
			p.add(")");
		}
	}
}
