package com.github.firmwehr.gentle.ast;

import java.util.List;

public record Program<I>(SourcePosition position, List<ClassDeclaration<I>> classDeclarations) implements HasSourcePosition {
}
