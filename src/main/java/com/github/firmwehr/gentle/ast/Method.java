package com.github.firmwehr.gentle.ast;

import com.github.firmwehr.gentle.ast.statement.BlockStatement;
import com.github.firmwehr.gentle.ast.type.Type;

import java.util.List;

public record Method<I>(SourcePosition position, Type<I> returnType, I name, List<Parameter<I>> parameters, BlockStatement<I> body) implements HasSourcePosition {
}
