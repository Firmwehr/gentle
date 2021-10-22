package com.github.firmwehr.gentle.ast;

public record Program<I>(SourcePosition position, List<ClassDeclaration<I>> classDeclarations) implements HasSourcePosition {
}
