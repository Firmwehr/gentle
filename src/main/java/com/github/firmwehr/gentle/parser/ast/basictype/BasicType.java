package com.github.firmwehr.gentle.parser.ast.basictype;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;

public sealed interface BasicType extends PrettyPrint permits IntType, BooleanType, VoidType, IdentType {
}

