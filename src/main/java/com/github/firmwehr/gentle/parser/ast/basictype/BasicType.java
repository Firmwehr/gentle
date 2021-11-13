package com.github.firmwehr.gentle.parser.ast.basictype;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.source.SourceSpan;

public sealed interface BasicType extends PrettyPrint permits IntType, BooleanType, VoidType, IdentType {
	SourceSpan sourceSpan();
}

