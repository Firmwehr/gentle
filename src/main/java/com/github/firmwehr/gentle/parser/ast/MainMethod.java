package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.ast.statement.Block;
import com.github.firmwehr.gentle.parser.ast.statement.Statement;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record MainMethod(
	Ident name,
	Parameter parameter,
	Block block
) implements PrettyPrint {
	public MainMethod(String name, Type paramType, String paramName) {
		this(new Ident(name), new Parameter(paramType, new Ident(paramName)), Statement.newBlock());
	}

	public MainMethod withBody(Block body) {
		return new MainMethod(name, parameter, body);
	}

	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("Method{").indent().newline();
		p.add("name = ").add(name).newline();
		p.add("parameter = ").add(parameter).newline();
		p.add("block = ").add(block).newline();
		p.unindent().add("}");
	}
}
