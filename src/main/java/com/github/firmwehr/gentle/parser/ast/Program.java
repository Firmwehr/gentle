package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.List;
import java.util.stream.Stream;

public record Program(List<ClassDeclaration> classes) implements PrettyPrint {
	public Program() {
		this(List.of());
	}

	public Program withDecl(ClassDeclaration declaration) {
		return new Program(Stream.concat(classes.stream(), Stream.of(declaration)).toList());
	}

	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("Program[").indent().addAll(classes).unindent().add("]");
	}
}
