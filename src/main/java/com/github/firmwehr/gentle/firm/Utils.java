package com.github.firmwehr.gentle.firm;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.expression.SLocalVariableExpression;
import com.github.firmwehr.gentle.semantic.ast.statement.SStatement;

import java.util.HashSet;
import java.util.Set;

public class Utils {
	public static int countLocalVars(SMethod method) throws SemanticException {
		Set<LocalVariableDeclaration> variables = new HashSet<>();

		Visitor<Void> visitor = new Visitor<>() {

			@Override
			public Void defaultReturnValue() {
				return null;
			}

			@Override
			public Void visit(SLocalVariableExpression localVariableExpression) {
				variables.add(localVariableExpression.localVariable());
				return null;
			}
		};
		for (SStatement statement : method.body()) {
			statement.accept(visitor);
		}
		return variables.size();
	}

}
