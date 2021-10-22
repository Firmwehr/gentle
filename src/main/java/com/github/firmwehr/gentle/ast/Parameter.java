package com.github.firmwehr.gentle.ast;

import com.github.firmwehr.gentle.ast.type.Type;

public record Parameter<I>(SourcePosition position, Type<I> type, I name) implements HasSourcePosition {
}
