package com.github.firmwehr.gentle.semantic.ast.statement;

import java.util.List;

public record Block(List<Statement> statements) implements Statement {
}
