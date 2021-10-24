package com.github.firmwehr.gentle.parser.ast;

import java.util.List;

public record ClassDeclaration(
	List<Field> fields,
	List<Method> methods,
	List<MainMethod> mainMethods
) {
}
