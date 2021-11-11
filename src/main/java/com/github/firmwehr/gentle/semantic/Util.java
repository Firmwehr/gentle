package com.github.firmwehr.gentle.semantic;

import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.ast.basictype.BooleanType;
import com.github.firmwehr.gentle.parser.ast.basictype.IdentType;
import com.github.firmwehr.gentle.parser.ast.basictype.IntType;
import com.github.firmwehr.gentle.parser.ast.basictype.VoidType;
import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.basictype.SBasicType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SBooleanType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SClassType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SIntType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SStringType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidType;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidyType;
import com.github.firmwehr.gentle.source.Source;

public final class Util {
	private Util() {
	}

	public static SNormalType normalTypeFromParserType(
		Source source, Namespace<SClassDeclaration> classes, Type type
	) throws SemanticException {
		SBasicType basicType = switch (type.basicType()) {
			case BooleanType t -> new SBooleanType();
			case IdentType t -> {
				if (t.name().ident().equals("String")) {
					yield new SStringType();
				} else {
					yield new SClassType(classes.get(t.name()));
				}
			}
			case IntType t -> new SIntType();
			case VoidType t -> throw new SemanticException(source, t.sourceSpan(), "void not allowed here");
		};

		return new SNormalType(basicType, type.arrayLevel());
	}

	public static SVoidyType voidyTypeFromParserType(
		Source source, Namespace<SClassDeclaration> classes, Type type
	) throws SemanticException {
		if (type.arrayLevel() == 0 && type.basicType() instanceof VoidType) {
			return new SVoidType();
		} else {
			return normalTypeFromParserType(source, classes, type);
		}
	}
}
