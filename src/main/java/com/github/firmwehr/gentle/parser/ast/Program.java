package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.Util;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public record Program(List<ClassDeclaration> classes) implements PrettyPrint {
	public Program() {
		this(List.of());
	}

	public Program withDecl(ClassDeclaration declaration) {
		return new Program(Util.copyAndAppend(classes, declaration));
	}

	@Override
	public void prettyPrint(PrettyPrinter p, Parentheses parens) {
		List<ClassDeclaration> classes =
			this.classes.stream().sorted(Comparator.comparing(c -> c.name().ident())).collect(Collectors.toList());

		// Format classes without trailing newline
		if (!classes.isEmpty()) {
			p.add(classes.get(0));
			classes.stream().skip(1).forEach(c -> p.newline().add(c));
		}
	}
}
