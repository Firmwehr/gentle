package com.github.firmwehr.gentle.semantic.ast.type;

public sealed interface SVoidyType permits SNormalType, SVoidType {
	SExprType asExprType();
}
