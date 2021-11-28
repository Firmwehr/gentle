package com.github.firmwehr.gentle.firm.construction;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.basictype.SBasicType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SBooleanType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SClassType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SIntType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SStringType;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.semantic.ast.type.SNullType;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidType;
import firm.ClassType;
import firm.Mode;
import firm.PointerType;
import firm.PrimitiveType;
import firm.Type;

import java.util.HashMap;
import java.util.Map;

public class TypeHelper {
	private final Type stringType;
	private final Type voidType;
	private final Map<SClassDeclaration, PointerType> classTypes;

	public TypeHelper() {
		this.stringType = new ClassType("String");
		this.classTypes = new HashMap<>();
		this.voidType = new PrimitiveType(Mode.getANY());
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
		return new PointerType(getType(normalType.withDecrementedLevel().orElseThrow()));
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

	public Mode getMode(SExprType returnType) {
		return switch (returnType) {
			case SNormalType normalType -> getMode(normalType);
			case SVoidType ignored -> Mode.getANY();
			case SNullType ignored -> throw new InternalCompilerException("tried to fetch mode for null type");
		};
	}

	public Type getType(SExprType returnType) {
		return switch (returnType) {
			case SNormalType normalType -> getType(normalType);
			case SVoidType ignored -> voidType;
			case SNullType ignored -> throw new InternalCompilerException("tried to fetch type for null type");
		};
	}
}
