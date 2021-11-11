package com.github.firmwehr.gentle.semantic;

import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SField;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.expression.SArrayAccessExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SBinaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SBooleanValueExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SFieldAccessExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SIntegerValueExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SLocalVariableExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SMethodInvocationExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SNewArrayExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SNewObjectExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SNullExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemInReadExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemOutFlushExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemOutPrintlnExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemOutWriteExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SThisExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SUnaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.statement.SBlock;
import com.github.firmwehr.gentle.semantic.ast.statement.SExpressionStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SIfStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SReturnStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SWhileStatement;

import java.util.Optional;

public interface Visitor<T> {

	//<editor-fold desc="Expressions">
	default Optional<T> visit(SArrayAccessExpression arrayExpression) throws SemanticException {
		arrayExpression.expression().accept(this);
		return Optional.empty();
	}

	default Optional<T> visit(SBinaryOperatorExpression binaryOperatorExpression) throws SemanticException {
		binaryOperatorExpression.lhs().accept(this);
		binaryOperatorExpression.rhs().accept(this);
		return Optional.empty();
	}

	default Optional<T> visit(SBooleanValueExpression booleanValueExpression) throws SemanticException {
		return Optional.empty();
	}

	default Optional<T> visit(SFieldAccessExpression fieldAccessExpression) throws SemanticException {
		fieldAccessExpression.expression().accept(this);
		return Optional.empty();
	}

	default Optional<T> visit(SIntegerValueExpression integerValueExpression) throws SemanticException {
		return Optional.empty();
	}

	default Optional<T> visit(SLocalVariableExpression localVariableExpression) throws SemanticException {
		return Optional.empty();
	}

	default Optional<T> visit(SMethodInvocationExpression methodInvocationExpression) throws SemanticException {
		methodInvocationExpression.expression().accept(this);
		for (SExpression argument : methodInvocationExpression.arguments()) {
			argument.accept(this);
		}
		return Optional.empty();
	}

	default Optional<T> visit(SNewArrayExpression newArrayExpression) throws SemanticException {
		newArrayExpression.size().accept(this);
		return Optional.empty();
	}

	default Optional<T> visit(SNewObjectExpression newObjectExpression) throws SemanticException {
		return Optional.empty();
	}

	default Optional<T> visit(SNullExpression nullExpression) throws SemanticException {
		return Optional.empty();
	}

	default Optional<T> visit(SSystemInReadExpression systemInReadExpression) throws SemanticException {
		return Optional.empty();
	}

	default Optional<T> visit(SSystemOutFlushExpression systemOutFlushExpression) throws SemanticException {
		return Optional.empty();
	}

	default Optional<T> visit(SSystemOutPrintlnExpression systemOutPrintlnExpression) throws SemanticException {
		systemOutPrintlnExpression.argument().accept(this);
		return Optional.empty();
	}

	default Optional<T> visit(SSystemOutWriteExpression systemOutWriteExpression) throws SemanticException {
		systemOutWriteExpression.argument().accept(this);
		return Optional.empty();
	}

	default Optional<T> visit(SThisExpression thisExpression) throws SemanticException {
		return Optional.empty();
	}

	default Optional<T> visit(SUnaryOperatorExpression unaryOperatorExpression) throws SemanticException {
		unaryOperatorExpression.expression().accept(this);
		return Optional.empty();
	}
	//</editor-fold>

	//<editor-fold desc="Statements">
	default Optional<T> visit(SBlock block) throws SemanticException {
		for (SStatement sStatement : block.statements()) {
			sStatement.accept(this);
		}
		return Optional.empty();
	}

	default Optional<T> visit(SExpressionStatement expressionStatement) throws SemanticException {
		expressionStatement.expression().accept(this);
		return Optional.empty();
	}

	default Optional<T> visit(SIfStatement ifStatement) throws SemanticException {
		ifStatement.condition().accept(this);
		ifStatement.body().accept(this);
		if (ifStatement.elseBody().isPresent()) {
			ifStatement.elseBody().get().accept(this);
		}
		return Optional.empty();
	}

	default Optional<T> visit(SReturnStatement returnStatement) throws SemanticException {
		if (returnStatement.returnValue().isPresent()) {
			returnStatement.returnValue().get().accept(this);
		}
		return Optional.empty();
	}

	default Optional<T> visit(SWhileStatement whileStatement) throws SemanticException {
		whileStatement.condition().accept(this);
		whileStatement.body().accept(this);
		return Optional.empty();
	}
	//</editor-fold>

	default Optional<T> visit(SMethod method) throws SemanticException {
		for (SStatement sStatement : method.body()) {
			sStatement.accept(this);
		}
		return Optional.empty();
	}

	default Optional<T> visit(SField field) {
		return Optional.empty();
	}

	default Optional<T> visit(SClassDeclaration classDeclaration) throws SemanticException {
		for (SField sField : classDeclaration.fields().getAll()) {
			visit(sField);
		}
		for (SMethod sMethod : classDeclaration.methods().getAll()) {
			visit(sMethod);
		}

		return Optional.empty();
	}
}
