package com.github.firmwehr.gentle.semantic;

import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.ast.basictype.BooleanType;
import com.github.firmwehr.gentle.parser.ast.basictype.IdentType;
import com.github.firmwehr.gentle.parser.ast.basictype.IntType;
import com.github.firmwehr.gentle.parser.ast.basictype.VoidType;
import com.github.firmwehr.gentle.parser.ast.expression.ArrayAccessExpression;
import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperator;
import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperatorExpression;
import com.github.firmwehr.gentle.parser.ast.expression.BooleanLiteralExpression;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.ast.expression.FieldAccessExpression;
import com.github.firmwehr.gentle.parser.ast.expression.IdentExpression;
import com.github.firmwehr.gentle.parser.ast.expression.IntegerLiteralExpression;
import com.github.firmwehr.gentle.parser.ast.expression.LocalMethodCallExpression;
import com.github.firmwehr.gentle.parser.ast.expression.MethodInvocationExpression;
import com.github.firmwehr.gentle.parser.ast.expression.NewArrayExpression;
import com.github.firmwehr.gentle.parser.ast.expression.NewObjectExpression;
import com.github.firmwehr.gentle.parser.ast.expression.NullExpression;
import com.github.firmwehr.gentle.parser.ast.expression.ThisExpression;
import com.github.firmwehr.gentle.parser.ast.expression.UnaryOperatorExpression;
import com.github.firmwehr.gentle.parser.ast.statement.Block;
import com.github.firmwehr.gentle.parser.ast.statement.BlockStatement;
import com.github.firmwehr.gentle.parser.ast.statement.EmptyStatement;
import com.github.firmwehr.gentle.parser.ast.statement.ExpressionStatement;
import com.github.firmwehr.gentle.parser.ast.statement.IfStatement;
import com.github.firmwehr.gentle.parser.ast.statement.LocalVariableDeclarationStatement;
import com.github.firmwehr.gentle.parser.ast.statement.ReturnStatement;
import com.github.firmwehr.gentle.parser.ast.statement.Statement;
import com.github.firmwehr.gentle.parser.ast.statement.WhileStatement;
import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SField;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.basictype.SBasicType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SBooleanType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SClassType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SIntType;
import com.github.firmwehr.gentle.semantic.ast.expression.SArrayAccessExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SBinaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SBooleanValueExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SFieldAccessExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SLocalVariableExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SThisExpression;
import com.github.firmwehr.gentle.semantic.ast.statement.SBlock;
import com.github.firmwehr.gentle.semantic.ast.statement.SExpressionStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SIfStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SReturnStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SWhileStatement;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.source.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record FunctionScope(
	Source source,
	Namespace<SClassDeclaration> classes,
	Optional<SClassDeclaration> currentClass,
	StackedNamespace<LocalVariableDeclaration> localVariables
) {
	public static FunctionScope fromMethod(Source source, Namespace<SClassDeclaration> classes, SMethod method)
		throws SemanticException {

		Optional<SClassDeclaration> currentClass =
			method.isStatic() ? Optional.empty() : Optional.of(method.classDecl());

		StackedNamespace<LocalVariableDeclaration> localVariables = new StackedNamespace<>(source);
		for (LocalVariableDeclaration parameter : method.parameters()) {
			localVariables.put(parameter.getDeclaration(), parameter);
		}

		return new FunctionScope(source, classes, currentClass, localVariables);
	}

	public SBlock convert(Block block) throws SemanticException {
		localVariables.enterScope();

		List<SStatement> statements = new ArrayList<>();
		for (BlockStatement statement : block.statements()) {
			switch (statement) {
				case Block s -> statements.add(convert(s));
				case EmptyStatement s -> {
				}
				case ExpressionStatement s -> statements.add(convert(s));
				case IfStatement s -> statements.add(convert(s));
				case LocalVariableDeclarationStatement s -> convert(s).ifPresent(statements::add);
				case ReturnStatement s -> statements.add(convert(s));
				case WhileStatement s -> statements.add(convert(s));
			}
		}

		localVariables.leaveScope();
		return new SBlock(statements);
	}

	public SStatement convert(Statement statement) throws SemanticException {
		return switch (statement) {
			case Block s -> convert(s);
			case EmptyStatement s -> new SBlock();
			case ExpressionStatement s -> convert(s);
			case IfStatement s -> convert(s);
			case ReturnStatement s -> convert(s);
			case WhileStatement s -> convert(s);
		};
	}

	SExpressionStatement convert(ExpressionStatement statement) throws SemanticException {
		return new SExpressionStatement(convert(statement.expression()));
	}

	SIfStatement convert(IfStatement statement) throws SemanticException {
		Optional<SStatement> elseBody;
		if (statement.elseBody().isPresent()) {
			elseBody = Optional.ofNullable(convert(statement.elseBody().get()));
		} else {
			elseBody = Optional.empty();
		}

		return new SIfStatement(convert(statement.condition()), convert(statement.body()), elseBody);
	}

	Optional<SExpressionStatement> convert(LocalVariableDeclarationStatement statement) throws SemanticException {
		LocalVariableDeclaration decl = new LocalVariableDeclaration(convertNormal(statement.type()),
			statement.name());

		localVariables.put(decl.getDeclaration(), decl);

		if (statement.value().isPresent()) {
			SExpression lhs = new SLocalVariableExpression(decl);
			SExpression rhs = convert(statement.value().get());
			return Optional.of(new SExpressionStatement(
				new SBinaryOperatorExpression(lhs, rhs, BinaryOperator.ASSIGN, decl.getType())));
		} else {
			return Optional.empty();
		}
	}

	SReturnStatement convert(ReturnStatement statement) throws SemanticException {
		Optional<SExpression> returnValue;
		if (statement.returnValue().isPresent()) {
			returnValue = Optional.of(convert(statement.returnValue().get()));
		} else {
			returnValue = Optional.empty();
		}

		return new SReturnStatement(returnValue);
	}

	SWhileStatement convert(WhileStatement statement) throws SemanticException {
		return new SWhileStatement(convert(statement.condition()), convert(statement.body()));
	}

	SNormalType convertNormal(Type type) throws SemanticException {
		// TODO Reduce code duplication with SemanticAnalyzer?
		SBasicType basicType = switch (type.basicType()) {
			case BooleanType t -> new SBooleanType();
			case IdentType t -> new SClassType(classes.get(t.name()));
			case IntType t -> new SIntType();
			case VoidType t -> throw new SemanticException(source, t.sourceSpan(), "void not allowed here");
		};

		return new SNormalType(basicType, type.arrayLevel());
	}

	SExpression convert(Expression expr) throws SemanticException {
		return switch (expr) {
			case ArrayAccessExpression e -> convert(e);
			case BinaryOperatorExpression e -> convert(e);
			case BooleanLiteralExpression e -> convert(e);
			case FieldAccessExpression e -> convert(e);
			case IdentExpression e -> convert(e);
			case IntegerLiteralExpression e -> convert(e);
			case LocalMethodCallExpression e -> convert(e);
			case MethodInvocationExpression e -> convert(e);
			case NewArrayExpression e -> convert(e);
			case NewObjectExpression e -> convert(e);
			case NullExpression e -> convert(e);
			case ThisExpression e -> convert(e);
			case UnaryOperatorExpression e -> convert(e);
		};
	}

	SArrayAccessExpression convert(ArrayAccessExpression expr) throws SemanticException {
		SExpression expression = convert(expr.expression());
		SExpression index = convert(expr.index());

		Optional<SNormalType> type = switch (expression.type()) {
			case SNormalType t -> t.withDecrementedLevel();
			default -> Optional.empty();
		};

		if (type.isEmpty()) {
			// TODO Give expressions a SourceSpan
			// TODO Get rid of null here
			throw new SemanticException(source, null, "expected array");
		}

		return new SArrayAccessExpression(expression, index, type.get());
	}

	SBinaryOperatorExpression convert(BinaryOperatorExpression expr) throws SemanticException {
		SExpression rhs = convert(expr.rhs());
		SExpression lhs = convert(expr.lhs());

		SExprType type = switch (expr.operator()) {
			case ASSIGN -> rhs.type();
			case LOGICAL_OR, LOGICAL_AND, EQUAL, NOT_EQUAL, LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> new SNormalType(
				new SBooleanType());
			case ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO -> new SNormalType(new SIntType());
		};

		return new SBinaryOperatorExpression(lhs, rhs, expr.operator(), type);
	}

	SBooleanValueExpression convert(BooleanLiteralExpression expr) {
		return new SBooleanValueExpression(expr.value());
	}

	SFieldAccessExpression convert(FieldAccessExpression expr) throws SemanticException {
		SExpression expression = convert(expr.expression());

		SField field;
		if (expression.type() instanceof SNormalType t && t.arrayLevel() == 0 &&
			t.basicType() instanceof SClassType classType) {

			field = classType.classDecl().fields().get(expr.name());
		} else {
			// TODO Get rid of null here
			throw new SemanticException(source, null, "expected object");
		}

		return new SFieldAccessExpression(expression, field);
	}

	SExpression convert(IdentExpression expr) throws SemanticException {
		if (currentClass.isPresent()) {
			if (localVariables.getOpt(expr.name()).isPresent()) {
				return new SLocalVariableExpression(localVariables.getOpt(expr.name()).get());
			} else {
				SField field = currentClass.get().fields().get(expr.name());
				return new SFieldAccessExpression(new SThisExpression(currentClass.get()), field);
			}
		} else {
			return new SLocalVariableExpression(localVariables().get(expr.name()));
		}
	}

	SExpression convert(IntegerLiteralExpression expr) {
		return null; // TODO Implement
	}

	SExpression convert(LocalMethodCallExpression expr) {
		return null; // TODO Implement
	}

	SExpression convert(MethodInvocationExpression expr) {
		return null; // TODO Implement
	}

	SExpression convert(NewArrayExpression expr) {
		return null; // TODO Implement
	}

	SExpression convert(NewObjectExpression expr) {
		return null; // TODO Implement
	}

	SExpression convert(NullExpression expr) {
		return null; // TODO Implement
	}

	SExpression convert(ThisExpression expr) {
		return null; // TODO Implement
	}

	SExpression convert(UnaryOperatorExpression expr) {
		return null; // TODO Implement
	}

}
