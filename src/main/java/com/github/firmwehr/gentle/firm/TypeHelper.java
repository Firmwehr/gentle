package com.github.firmwehr.gentle.firm;

import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.basictype.SBasicType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SBooleanType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SClassType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SIntType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SStringType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import firm.ClassType;
import firm.Mode;
import firm.PointerType;
import firm.Type;

import java.util.HashMap;
import java.util.Map;

public class TypeHelper {

	private final Type stringType;
	private final Map<SClassDeclaration, PointerType> classTypes;

	public TypeHelper() {
		this.stringType = new ClassType("String");
		this.classTypes = new HashMap<>();
	}

	public Type getType(SBasicType basicType) {
		return switch (basicType) {
			case SBooleanType ignored -> Mode.getBu().getType();
			case SIntType ignored -> Mode.getIs().getType();
			case SStringType ignored -> getStringType();
			case SClassType classType -> getType(classType.classDecl());
		};
	}

	public Type getType(SNormalType normalType) {
		if (normalType.arrayLevel() == 0) {
			return getType(normalType.basicType());
		}
		return new PointerType(getType(normalType.withDecrementedLevel().get()));
	}

	public Mode getMode(SBasicType basicType) {
		return switch (basicType) {
			case SBooleanType ignored -> Mode.getBu();
			case SIntType ignored -> Mode.getIs();
			case SStringType ignored -> Mode.getP();
			case SClassType ignored -> Mode.getP();
		};

	}

	public Mode getMode(SNormalType normalType) {
		if (normalType.arrayLevel() == 0) {
			return getMode(normalType.basicType());
		}
		return Mode.getP();
	}

	public PointerType getType(SClassDeclaration classDeclaration) {
		return classTypes.computeIfAbsent(classDeclaration,
			decl -> new PointerType(new ClassType(decl.name().ident())));
	}

	public ClassType getClassType(SClassDeclaration classDeclaration) {
		return (ClassType) classTypes.computeIfAbsent(classDeclaration,
			decl -> new PointerType(new ClassType(decl.name().ident()))).getPointsTo();
	}

	public Type getStringType() {
		return stringType;
	}
}
