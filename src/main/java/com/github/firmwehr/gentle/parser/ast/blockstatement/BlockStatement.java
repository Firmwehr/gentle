package com.github.firmwehr.gentle.parser.ast.blockstatement;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;

public sealed interface BlockStatement extends PrettyPrint permits JustAStatement, LocalVariableDeclarationStatement {
}
