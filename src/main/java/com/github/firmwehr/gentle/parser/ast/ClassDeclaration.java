package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.List;
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
		List<Field> newFields = Stream.concat(fields.stream(), Stream.of(new Field(type, new Ident(name)))).toList();
		return new ClassDeclaration(this.name, newFields, methods, mainMethods);
	}

	public ClassDeclaration withMethod(Method method) {
		List<Method> newMethods = Stream.concat(methods.stream(), Stream.of(method)).toList();
		return new ClassDeclaration(name, fields, newMethods, mainMethods);
	}


	public ClassDeclaration withMainMethod(MainMethod mainMethod) {
		List<MainMethod> newMainMethods = Stream.concat(mainMethods.stream(), Stream.of(mainMethod)).toList();
		return new ClassDeclaration(name, fields, methods, newMainMethods);
	}

	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("ClassDeclaration{").indent().newline();
		p.add("fields = [").indent().addAll(fields).unindent().add("]").newline();
		p.add("methods = [").indent().addAll(methods).unindent().add("]").newline();
		p.add("mainMethods = [").indent().addAll(mainMethods).unindent().add("]").newline();
		p.unindent().add("}");
	}
}
