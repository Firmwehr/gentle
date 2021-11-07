package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.ast.statement.Block;
import com.github.firmwehr.gentle.parser.ast.statement.Statement;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record MainMethod(
	Ident name,
	Parameter parameter,
	Block body
) implements PrettyPrint {
	public MainMethod(String name, Type paramType, String paramName) {
		this(new Ident(name), new Parameter(paramType, new Ident(paramName)), Statement.newBlock());
	}

	public MainMethod withBody(Block body) {
		return new MainMethod(name, parameter, body);
	}

	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		p.add("public static void ").add(name).add("(").add(parameter).add(") ").add(body);
	}
}
