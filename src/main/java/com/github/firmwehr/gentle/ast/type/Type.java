package com.github.firmwehr.gentle.ast.type;

import com.github.firmwehr.gentle.ast.HasSourcePosition;

public sealed interface Type<I> extends HasSourcePosition permits ArrayType, IntType, BooleanType, ClassType {
}