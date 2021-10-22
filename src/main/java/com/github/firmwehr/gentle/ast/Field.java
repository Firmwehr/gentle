package com.github.firmwehr.gentle.ast;

import com.github.firmwehr.gentle.ast.type.Type;

public record Field<I>(SourcePosition position, I identifier, Type<I> type) implements HasSourcePosition {
}