package com.github.firmwehr.gentle.ast;

import java.util.List;

public record ClassDeclaration<I>(SourcePosition position, I className, List<Field<I>> fields, List<Method<I>> methods) {
}