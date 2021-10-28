package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.ast.statement.Block;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.List;

public record Method(
	Type returnType,
	Ident name,
	List<Parameter> parameters,
	Block block
) implements PrettyPrint {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("Method{").indent().newline();
		p.add("returnType = ").add(returnType).newline();
		p.add("name = ").add(name).newline();
		p.add("parameters = [").indent().addAll(parameters).unindent().add("]").newline();
		p.add("block = ").add(block).newline();
		p.unindent().add("}");
	}
}
