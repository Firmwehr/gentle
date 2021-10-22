package com.github.firmwehr.gentle.ast.statement;

import com.github.firmwehr.gentle.ast.SourcePosition;

import java.util.List;

public record BlockStatement<I>(SourcePosition position, List<Statement<I>> statements) implements Statement<I> {
}
