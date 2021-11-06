package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.Util;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ClassDeclaration(
	Ident name,
	List<Field> fields,
	List<Method> methods,
	List<MainMethod> mainMethods
) implements PrettyPrint {
	public ClassDeclaration(String name) {
		this(new Ident(name), List.of(), List.of(), List.of());
	}

	public ClassDeclaration withField(Type type, String name) {
		Field field = new Field(type, new Ident(name));
		return new ClassDeclaration(this.name, Util.copyAndAppend(fields, field), methods, mainMethods);
	}

	public ClassDeclaration withMethod(Method method) {
		return new ClassDeclaration(name, fields, Util.copyAndAppend(methods, method), mainMethods);
	}


	public ClassDeclaration withMainMethod(MainMethod mainMethod) {
		return new ClassDeclaration(name, fields, methods, Util.copyAndAppend(mainMethods, mainMethod));
	}

	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		List<Field> fields = this.fields.stream()
			.sorted(Comparator.comparing(field -> field.name().ident()))
			.collect(Collectors.toList());

		List<PrettyPrint> methods =
			Stream.concat(this.methods.stream(), this.mainMethods.stream()).sorted(Comparator.comparing(it -> {
				// Ugly, but the easiest thing I could think of right now
				if (it instanceof Method m) {
					return m.name().ident();
				} else if (it instanceof MainMethod m) {
					return m.name().ident();
				} else {
					throw new IllegalArgumentException("expected Method or MainMethod");
				}
			})).collect(Collectors.toList());

		p.add("class ").add(name).add(" ");

		if (fields.isEmpty() && methods.isEmpty()) {
			// Strictly speaking, this is not a block, but since the behavior for empty class bodies was never
			// specified, I'm formatting them like blocks.
			p.add("{ }");
		} else {
			p.add("{").indent().newline();
			p.addAll(methods, "", true);
			p.addAll(fields, "", true);
			p.unindent().add("}");
		}
	}
}
