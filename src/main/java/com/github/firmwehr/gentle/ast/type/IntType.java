package com.github.firmwehr.gentle.ast.type;

import com.github.firmwehr.gentle.ast.SourcePosition;

public record IntType<I>(SourcePosition position) implements Type<I> {
}