package com.github.firmwehr.gentle.parser.ast.blockstatement;

import com.github.firmwehr.gentle.parser.ast.statement.Statement;

public record JustAStatement(Statement statement) implements BlockStatement {
}
