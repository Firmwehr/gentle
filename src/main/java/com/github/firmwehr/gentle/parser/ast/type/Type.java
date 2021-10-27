package com.github.firmwehr.gentle.parser.ast.type;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;

public sealed interface Type extends PrettyPrint permits IntType, BooleanType, VoidType, IdentType, ArrayType {
}

