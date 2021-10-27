package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.List;

public record ClassDeclaration(
	Ident name,
	List<Field> fields,
	List<Method> methods,
	List<MainMethod> mainMethods
) implements PrettyPrint {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("ClassDeclaration{").indent().newline();
		p.add("fields = [").indent().addAll(fields).unindent().add("]").newline();
		p.add("methods = [").indent().addAll(methods).unindent().add("]").newline();
		p.add("mainMethods = [").indent().addAll(mainMethods).unindent().add("]").newline();
		p.unindent().add("}");
	}
}
