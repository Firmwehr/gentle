package com.github.firmwehr.gentle.semantic.analysis;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.statement.SBlock;
import com.github.firmwehr.gentle.semantic.ast.statement.SIfStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SReturnStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SStatement;
import com.github.firmwehr.gentle.source.Source;

import java.util.Optional;

/**
 * Ensures every method is either void or every path leads to a return statement.
 * <br>
 * This is implemented using a simple algorithm:
 * <ol>
 *     <li>For every statement in a method or block, check whether it always returns. If it always returns, the
 *     method always returns as any potentially following code is unreachable.</li>
 *     <li>If we reached the end of the method body the method doesn't always return and we throw an exception.</li>>
 * </ol>
 * Additionally, the {@link SReturnStatement} is marked to always return and acts as the starting point.
 */
@SuppressWarnings("ClassCanBeRecord")
public class MethodReturnsVisitor implements Visitor<MethodReturnsVisitor.Returns> {
	private final Source source;

	public MethodReturnsVisitor(Source source) {
		this.source = source;
	}

	@Override
	public Optional<Returns> visit(SMethod method) throws SemanticException {
		// We do not need to check void methods. If they have a return statement it is verified by type
		// checking and any path not explicitly returning just implicitly ends.
		if (method.returnType().asExprType().asVoidType().isPresent()) {
			return Optional.of(Returns.YES);
		}

		for (SStatement statement : method.body()) {
			// This statement always returns so the rest is unreachable code, and we can bail out.
			if (statement.accept(this).isPresent()) {
				return Optional.of(Returns.YES);
			}
		}

		throw new SemanticException(source, null, "Found path not leading to return");
	}

	@Override
	public Optional<Returns> visit(SIfStatement ifStatement) throws SemanticException {
		if (ifStatement.body().accept(this).isEmpty()) {
			return Optional.empty();
		}
		// The body always returns, but we have no else => This might not return if the condition isn't always true
		if (ifStatement.elseBody().isEmpty()) {
			return Optional.empty();
		}
		return ifStatement.elseBody().get().accept(this);
	}

	@Override
	public Optional<Returns> visit(SReturnStatement returnStatement) {
		return Optional.of(Returns.YES);
	}

	@Override
	public Optional<Returns> visit(SBlock block) throws SemanticException {
		for (SStatement statement : block.statements()) {
			// This statement always returns so the rest is unreachable code, and we can bail out.
			if (statement.accept(this).isPresent()) {
				return Optional.of(Returns.YES);
			}
		}
		return Optional.empty();
	}

	public enum Returns {
		YES
	}
}
