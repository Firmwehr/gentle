package com.github.firmwehr.gentle.semantic.analysis;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.statement.SBlock;
import com.github.firmwehr.gentle.semantic.ast.statement.SIfStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SReturnStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SWhileStatement;
import com.github.firmwehr.gentle.source.Source;

/**
 * Ensures every method is either void or every path leads to a return statement.
 * <br>
 * This is implemented using a simple algorithm:
 * <ol>
 *     <li>If any statement in a method or block always returns, that method or block always returns. All following
 *     statements are unreachable.</li>
 *     <li>Otherwise, the method or block doesn't always return.</li>
 * </ol>
 * Additionally, the {@link SReturnStatement} is marked to always return and acts as the starting point.
 */
public class MethodReturnsVisitor implements Visitor<MethodReturnsVisitor.AlwaysReturns> {
	private final Source source;

	public MethodReturnsVisitor(Source source) {
		this.source = source;
	}

	@Override
	public AlwaysReturns defaultReturnValue() {
		return AlwaysReturns.NO;
	}

	@Override
	public AlwaysReturns visit(SMethod method) throws SemanticException {
		// We do not need to check void methods. If they have a return statement it is verified by type
		// checking and any path not explicitly returning just implicitly ends.
		if (method.returnType().asExprType().asVoidType().isPresent()) {
			return AlwaysReturns.YES;
		}

		for (SStatement statement : method.body()) {
			// This statement always returns so the rest is unreachable code, and we can bail out.
			if (statement.accept(this) == AlwaysReturns.YES) {
				return AlwaysReturns.YES;
			}
		}

		throw new SemanticException(source, method.name().sourceSpan(), "missing return statement");
	}

	@Override
	public AlwaysReturns visit(SIfStatement ifStatement) throws SemanticException {
		if (ifStatement.body().accept(this) == AlwaysReturns.NO) {
			return AlwaysReturns.NO;
		}
		// The body always returns, but we have no else => This might not return if the condition isn't always true
		if (ifStatement.elseBody().isEmpty()) {
			return AlwaysReturns.NO;
		}
		return ifStatement.elseBody().get().accept(this);
	}

	@Override
	public AlwaysReturns visit(SWhileStatement whileStatement) throws SemanticException {
		// We do not need to visit a "while" statement as the condition might be false and therefore it won't
		// *definitely* return.
		return AlwaysReturns.NO;
	}

	@Override
	public AlwaysReturns visit(SReturnStatement returnStatement) {
		return AlwaysReturns.YES;
	}

	@Override
	public AlwaysReturns visit(SBlock block) throws SemanticException {
		for (SStatement statement : block.statements()) {
			// This statement always returns so the rest is unreachable code, and we can bail out.
			if (statement.accept(this) == AlwaysReturns.YES) {
				return AlwaysReturns.YES;
			}
		}
		return AlwaysReturns.NO;
	}

	public enum AlwaysReturns {
		YES,
		NO
	}
}
