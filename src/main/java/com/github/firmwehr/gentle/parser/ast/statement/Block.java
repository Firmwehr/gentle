package com.github.firmwehr.gentle.parser.ast.statement;

import com.github.firmwehr.gentle.parser.ast.blockstatement.BlockStatement;

import java.util.List;

public record Block(List<BlockStatement> statements) implements Statement {
}
