package com.github.firmwehr.gentle.semantic;

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
import com.github.firmwehr.gentle.semantic.ast.basictype.SBooleanType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SClassType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SIntType;
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
import com.github.firmwehr.gentle.semantic.ast.expression.SThisExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SUnaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.statement.SBlock;
import com.github.firmwehr.gentle.semantic.ast.statement.SExpressionStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SIfStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SReturnStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SWhileStatement;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.source.Source;
import com.github.firmwehr.gentle.source.SourceSpan;

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
		SNormalType type = Util.normalTypeFromParserType(source, classes, statement.type());
		LocalVariableDeclaration decl =
			new LocalVariableDeclaration(type, statement.type().sourceSpan(), statement.name());

		localVariables.put(decl.getDeclaration(), decl);

		if (statement.value().isPresent()) {
			SExpression lhs = new SLocalVariableExpression(decl, statement.name().sourceSpan());
			SExpression rhs = convert(statement.value().get().expression());
			SourceSpan span = SourceSpan.from(lhs.sourceSpan(), statement.value().get().parenSourceSpan());
			return Optional.of(new SExpressionStatement(
				new SBinaryOperatorExpression(lhs, rhs, BinaryOperator.ASSIGN, decl.getType(), span)));
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

		return new SReturnStatement(returnValue, statement.sourceSpan());
	}

	SWhileStatement convert(WhileStatement statement) throws SemanticException {
		return new SWhileStatement(convert(statement.condition()), convert(statement.body()));
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
			throw new SemanticException(source, expr.sourceSpan(), "expected array");
		}

		return new SArrayAccessExpression(expression, index, type.get(), expr.sourceSpan());
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

		return new SBinaryOperatorExpression(lhs, rhs, expr.operator(), type, expr.sourceSpan());
	}

	SBooleanValueExpression convert(BooleanLiteralExpression expr) {
		return new SBooleanValueExpression(expr.value(), expr.sourceSpan());
	}

	SFieldAccessExpression convert(FieldAccessExpression expr) throws SemanticException {
		SExpression expression = convert(expr.expression());

		Optional<SClassType> classType = typeToClassType(expression.type());
		if (classType.isEmpty()) {
			throw new SemanticException(source, expr.sourceSpan(), "expected object");
		}

		SField field = classType.get().classDecl().fields().get(expr.name());

		return new SFieldAccessExpression(expression, field, expr.sourceSpan());
	}

	SExpression convert(IdentExpression expr) throws SemanticException {
		if (currentClass.isPresent()) {
			if (localVariables.getOpt(expr.name()).isPresent()) {
				return new SLocalVariableExpression(localVariables.getOpt(expr.name()).get(), expr.sourceSpan());
			} else {
				SField field = currentClass.get().fields().get(expr.name());
				// The SThisExpression doesn't really have a proper SourceSpan. Giving it the IdentExpression's span
				// probably makes the most sense.
				return new SFieldAccessExpression(new SThisExpression(currentClass.get(), expr.sourceSpan()), field,
					expr.sourceSpan());
			}
		} else {
			return new SLocalVariableExpression(localVariables().get(expr.name()), expr.sourceSpan());
		}
	}

	SIntegerValueExpression convert(IntegerLiteralExpression expr) throws SemanticException {
		try {
			return new SIntegerValueExpression(expr.value().intValueExact(), expr.sourceSpan());
		} catch (ArithmeticException e) {
			throw new SemanticException(source, expr.sourceSpan(), "integer literal too large");
		}
	}

	SMethodInvocationExpression convert(LocalMethodCallExpression expr) throws SemanticException {
		if (currentClass.isEmpty()) {
			throw new SemanticException(source, expr.name().sourceSpan(), "calling local method in static context");
		}

		SMethod method = currentClass.get().methods().get(expr.name());
		if (method.isStatic()) {
			throw new SemanticException(source, expr.name().sourceSpan(), "calling main method");
		}

		List<SExpression> arguments = new ArrayList<>();
		for (Expression argument : expr.arguments()) {
			arguments.add(convert(argument));
		}

		// The SThisExpression doesn't really have a proper SourceSpan. Giving it the SMethodInvocationExpression's
		// span probably makes the most sense.
		return new SMethodInvocationExpression(new SThisExpression(currentClass.get(), expr.sourceSpan()), method,
			arguments, expr.sourceSpan());
	}

	SMethodInvocationExpression convert(MethodInvocationExpression expr) throws SemanticException {
		SExpression expression = convert(expr.expression());

		Optional<SClassType> classType = typeToClassType(expression.type());
		if (classType.isEmpty()) {
			throw new SemanticException(source, expr.sourceSpan(), "expected object");
		}

		SMethod method = classType.get().classDecl().methods().get(expr.name());
		if (method.isStatic()) {
			throw new SemanticException(source, expr.name().sourceSpan(), "calling main method");
		}

		List<SExpression> arguments = new ArrayList<>();
		for (Expression argument : expr.arguments()) {
			arguments.add(convert(argument));
		}

		return new SMethodInvocationExpression(expression, method, arguments, expr.sourceSpan());
	}

	SNewArrayExpression convert(NewArrayExpression expr) throws SemanticException {
		SNormalType type = Util.normalTypeFromParserType(source, classes, expr.type());
		return new SNewArrayExpression(type, convert(expr.size()), expr.sourceSpan());
	}

	SNewObjectExpression convert(NewObjectExpression expr) throws SemanticException {
		return new SNewObjectExpression(classes.get(expr.name()), expr.sourceSpan());
	}

	SNullExpression convert(NullExpression expr) {
		return new SNullExpression(expr.sourceSpan());
	}

	SThisExpression convert(ThisExpression expr) throws SemanticException {
		if (currentClass.isEmpty()) {
			throw new SemanticException(source, expr.sourceSpan(), "using 'this' in static context");
		}

		return new SThisExpression(currentClass.get(), expr.sourceSpan());
	}

	SUnaryOperatorExpression convert(UnaryOperatorExpression expr) throws SemanticException {
		return new SUnaryOperatorExpression(expr.operator(), convert(expr.expression()), expr.sourceSpan());
	}

	Optional<SClassType> typeToClassType(SExprType type) {
		if (type instanceof SNormalType t && t.arrayLevel() == 0 && t.basicType() instanceof SClassType classType) {
			return Optional.of(classType);
		} else {
			return Optional.empty();
		}
	}
}
