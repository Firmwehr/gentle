package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.Util;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.Comparator;
import java.util.List;

public record Program(List<ClassDeclaration> classes) implements PrettyPrint {
	public Program() {
		this(List.of());
	}

	public Program withDecl(ClassDeclaration declaration) {
		return new Program(Util.copyAndAppend(classes, declaration));
	}

	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		this.classes.stream().sorted(Comparator.comparing(c -> c.name().ident())).forEach(c -> p.add(c).newline());
	}
}
