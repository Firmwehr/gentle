package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.ast.statement.Block;
import com.github.firmwehr.gentle.parser.ast.statement.Statement;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.List;
import java.util.stream.Stream;

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
		List<Parameter> newParameters =
			Stream.concat(parameters.stream(), Stream.of(new Parameter(type, new Ident(name)))).toList();
		return new Method(returnType, this.name, newParameters, body);
	}

	public Method withBody(Block body) {
		return new Method(returnType, name, parameters, body);
	}

	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("Method{").indent().newline();
		p.add("returnType = ").add(returnType).newline();
		p.add("name = ").add(name).newline();
		p.add("parameters = [").indent().addAll(parameters).unindent().add("]").newline();
		p.add("body = ").add(body).newline();
		p.unindent().add("}");
	}
}
