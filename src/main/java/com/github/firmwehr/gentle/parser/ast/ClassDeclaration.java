package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.Util;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.List;

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
	public void prettyPrint(PrettyPrinter p) {
		p.add("ClassDeclaration{").indent().newline();
		p.add("fields = [").indent().addAll(fields).unindent().add("]").newline();
		p.add("methods = [").indent().addAll(methods).unindent().add("]").newline();
		p.add("mainMethods = [").indent().addAll(mainMethods).unindent().add("]").newline();
		p.unindent().add("}");
	}
}
