package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.List;

public record Program(List<ClassDeclaration> classes) implements PrettyPrint {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("Program[").indent().addAll(classes).unindent().add("]");
	}
}
