package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.Util;
import com.github.firmwehr.gentle.parser.ast.statement.Block;
import com.github.firmwehr.gentle.parser.ast.statement.Statement;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.List;

public record Method(
	Type returnType,
	Ident name,
	List<Parameter> parameters,
	Block body
) implements PrettyPrint {
	public Method(String name) {
		this(Type.newVoid(), new Ident(name), List.of(), Statement.newBlock());
	}

	public Method returning(Type returnType) {
		return new Method(returnType, name, parameters, body);
	}

	public Method withParam(Type type, String name) {
		Parameter parameter = new Parameter(type, new Ident(name));
		return new Method(returnType, this.name, Util.copyAndAppend(parameters, parameter), body);
	}

	public Method withBody(Block body) {
		return new Method(returnType, name, parameters, body);
	}

	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		p.add("public ")
			.add(returnType)
			.add(" ")
			.add(name)
			.add("(")
			.addAll(parameters, ", ", false)
			.add(") ")
			.add(body);
	}
}
