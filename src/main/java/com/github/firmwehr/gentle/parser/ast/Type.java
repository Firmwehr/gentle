package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.ast.basictype.BasicType;
import com.github.firmwehr.gentle.parser.ast.basictype.BooleanType;
import com.github.firmwehr.gentle.parser.ast.basictype.IdentType;
import com.github.firmwehr.gentle.parser.ast.basictype.IntType;
import com.github.firmwehr.gentle.parser.ast.basictype.VoidType;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.google.common.base.Preconditions;

public record Type(
	BasicType basicType,
	int arrayLevel
) implements PrettyPrint {
	public Type {
		Preconditions.checkArgument(arrayLevel >= 0);
	}

	public static Type newBool() {
		return new Type(new BooleanType(), 0);
	}

	public static Type newIdent(String name) {
		return new Type(new IdentType(new Ident(name)), 0);
	}

	public static Type newInt() {
		return new Type(new IntType(), 0);
	}

	public static Type newVoid() {
		return new Type(new VoidType(), 0);
	}

	public Type atLevel(int level) {
		return new Type(basicType, level);
	}

	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		p.add(basicType);
		for (int i = 0; i < arrayLevel; i++) {
			p.add("[]");
		}
	}
}
