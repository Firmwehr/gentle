package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.ast.basictype.BasicType;
import com.github.firmwehr.gentle.parser.ast.basictype.BooleanType;
import com.github.firmwehr.gentle.parser.ast.basictype.IdentType;
import com.github.firmwehr.gentle.parser.ast.basictype.IntType;
import com.github.firmwehr.gentle.parser.ast.basictype.VoidType;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.SourceSpan;
import com.google.common.base.Preconditions;

public record Type(
	BasicType basicType,
	int arrayLevel,
	SourceSpan sourceSpan
) implements PrettyPrint {
	public Type {
		Preconditions.checkArgument(arrayLevel >= 0);
	}

	public static Type newBool() {
		return new Type(new BooleanType(SourceSpan.dummy()), 0, SourceSpan.dummy());
	}

	public static Type newIdent(String name) {
		return new Type(new IdentType(Ident.dummy(name)), 0, SourceSpan.dummy());
	}

	public static Type newInt() {
		return new Type(new IntType(SourceSpan.dummy()), 0, SourceSpan.dummy());
	}

	public static Type newVoid() {
		return new Type(new VoidType(SourceSpan.dummy()), 0, SourceSpan.dummy());
	}

	public Type atLevel(int level) {
		return new Type(basicType, level, SourceSpan.dummy());
	}

	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		p.add(basicType);
		for (int i = 0; i < arrayLevel; i++) {
			p.add("[]");
		}
	}
}
