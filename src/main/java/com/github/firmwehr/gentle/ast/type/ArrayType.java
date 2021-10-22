package com.github.firmwehr.gentle.ast.type;

import com.github.firmwehr.gentle.ast.SourcePosition;

public record ArrayType<I>(SourcePosition position, Type<I> memberType) implements Type<I> {
}
