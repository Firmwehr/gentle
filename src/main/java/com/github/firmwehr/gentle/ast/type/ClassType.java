package com.github.firmwehr.gentle.ast.type;

import com.github.firmwehr.gentle.ast.SourcePosition;

public record ClassType<I>(SourcePosition position, I className) implements Type<I> {
}
