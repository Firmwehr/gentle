package com.github.firmwehr.gentle.semantic;

import com.github.firmwehr.gentle.parser.ast.ClassDeclaration;
import com.github.firmwehr.gentle.parser.ast.Field;
import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.MainMethod;
import com.github.firmwehr.gentle.parser.ast.Method;
import com.github.firmwehr.gentle.parser.ast.Parameter;
import com.github.firmwehr.gentle.parser.ast.Program;
import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.ast.basictype.BooleanType;
import com.github.firmwehr.gentle.parser.ast.basictype.IdentType;
import com.github.firmwehr.gentle.parser.ast.basictype.IntType;
import com.github.firmwehr.gentle.parser.ast.basictype.VoidType;
import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperator;
import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SField;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.SProgram;
import com.github.firmwehr.gentle.semantic.ast.basictype.SBasicType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SBooleanType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SClassType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SIntType;
import com.github.firmwehr.gentle.semantic.ast.expression.SArrayAccessExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SBinaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SLocalVariableExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SMethodInvocationExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SNewArrayExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SNewObjectExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemOutPrinlnExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemOutWriteExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SUnaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.statement.SExpressionStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SIfStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SReturnStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SWhileStatement;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidType;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidyType;
import com.github.firmwehr.gentle.source.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SemanticAnalyzer {
	private final Source source;
	private final Program program;

	public SemanticAnalyzer(Source source, Program program) {
		this.source = source;
		this.program = program;
	}

	SProgram analyze() throws SemanticException {
		Namespace<SClassDeclaration> classes = new Namespace<>(source);

		addClasses(classes);
		addFieldsAndFunctions(classes);
		addFunctionBodies(classes);

		checkTypes(classes);
		checkSideEffects(classes);
		checkAssignments(classes);
		checkReturnPaths(classes);

		SMethod mainMethod = findMainMethod(classes);

		return new SProgram(classes, mainMethod);
	}

	private void addClasses(Namespace<SClassDeclaration> classes) throws SemanticException {
		for (ClassDeclaration classDecl : program.classes()) {
			Ident name = classDecl.name();

			if (name.ident().equals("String")) {
				throw new SemanticException(source, name.sourceSpan(),
					"invalid name, already defined by built-in class");
			}

			SClassDeclaration sClassDecl =
				new SClassDeclaration(name, new Namespace<>(source), new Namespace<>(source));
			classes.put(name, sClassDecl);
		}
	}

	private SNormalType normalTypeFromParserType(Namespace<SClassDeclaration> classes, Type type)
		throws SemanticException {

		SBasicType basicType = switch (type.basicType()) {
			case BooleanType t -> new SBooleanType();
			case IdentType t -> new SClassType(classes.get(t.name()));
			case IntType t -> new SIntType();
			case VoidType t -> throw new SemanticException(source, t.sourceSpan(), "void not allowed here");
		};

		return new SNormalType(basicType, type.arrayLevel());
	}

	private SVoidyType voidyTypeFromParserType(Namespace<SClassDeclaration> classes, Type type)
		throws SemanticException {
		return switch (type.basicType()) {
			case BooleanType t -> new SNormalType(new SBooleanType(), type.arrayLevel());
			case IdentType t -> new SNormalType(new SClassType(classes.get(t.name())), type.arrayLevel());
			case IntType t -> new SNormalType(new SIntType(), type.arrayLevel());
			case VoidType t -> {
				if (type.arrayLevel() > 0) {
					throw new SemanticException(source, t.sourceSpan(), "void not allowed here");
				}
				yield new SVoidType();
			}
		};
	}

	private void addFieldsAndFunctions(Namespace<SClassDeclaration> classes) throws SemanticException {
		for (ClassDeclaration classDecl : program.classes()) {
			SClassDeclaration sClassDecl = classes.get(classDecl.name());

			for (Field field : classDecl.fields()) {
				Ident name = field.name();
				SNormalType type = normalTypeFromParserType(classes, field.type());
				SField sField = new SField(sClassDecl, name, type);
				sClassDecl.fields().put(name, sField);
			}

			for (Method method : classDecl.methods()) {
				Ident name = method.name();
				SVoidyType returnType = voidyTypeFromParserType(classes, method.returnType());

				List<LocalVariableDeclaration> parameters = new ArrayList<>();
				for (Parameter parameter : method.parameters()) {
					SNormalType type = normalTypeFromParserType(classes, parameter.type());
					parameters.add(new LocalVariableDeclaration(type, parameter.name()));
				}

				SMethod sMethod = SMethod.newMethod(sClassDecl, false, name, returnType, parameters);
				sClassDecl.methods().put(name, sMethod);
			}

			for (MainMethod mainMethod : classDecl.mainMethods()) {
				Ident name = mainMethod.name();

				SNormalType paramType = normalTypeFromParserType(classes, mainMethod.parameter().type());
				Ident paramName = mainMethod.parameter().name();
				LocalVariableDeclaration parameter = new LocalVariableDeclaration(paramType, paramName);

				SMethod sMethod = SMethod.newMethod(sClassDecl, true, name, new SVoidType(), List.of(parameter));
				sClassDecl.methods().put(name, sMethod);
			}
		}
	}

	void addFunctionBodies(Namespace<SClassDeclaration> classes) throws SemanticException {
		for (ClassDeclaration classDecl : program.classes()) {
			SClassDeclaration sClassDecl = classes.get(classDecl.name());

			for (Method method : classDecl.methods()) {
				SMethod sMethod = sClassDecl.methods().get(method.name());
				FunctionScope scope = FunctionScope.fromMethod(source, classes, sMethod);
				sMethod.body().addAll(scope.convert(method.body()).statements());
			}

			for (MainMethod mainMethod : classDecl.mainMethods()) {
				SMethod sMethod = sClassDecl.methods().get(mainMethod.name());
				FunctionScope scope = FunctionScope.fromMethod(source, classes, sMethod);
				sMethod.body().addAll(scope.convert(mainMethod.body()).statements());
			}
		}
	}

	void checkTypes(Namespace<SClassDeclaration> classes) throws SemanticException {
		// TODO Implement
		// TODO Don't forget the String type
		Visitor<Void> visitor = new Visitor<>() {
			SMethod currentMethod;

			@Override
			public Optional<Void> visit(SMethod method) throws SemanticException {
				this.currentMethod = method;
				return Visitor.super.visit(method);
			}

			@Override
			public Optional<Void> visit(SIfStatement ifStatement) throws SemanticException {
				assertIsBoolean(ifStatement.condition());

				return Visitor.super.visit(ifStatement);
			}

			@Override
			public Optional<Void> visit(SWhileStatement whileStatement) throws SemanticException {
				assertIsBoolean(whileStatement.condition());
				return Visitor.super.visit(whileStatement);
			}

			@Override
			public Optional<Void> visit(SArrayAccessExpression arrayExpression) throws SemanticException {
				assertIsInt(arrayExpression.expression());
				return Visitor.super.visit(arrayExpression);
			}

			@Override
			public Optional<Void> visit(SNewArrayExpression newArrayExpression) throws SemanticException {
				assertIsInt(newArrayExpression.size());
				return Visitor.super.visit(newArrayExpression);
			}

			@Override
			public Optional<Void> visit(SUnaryOperatorExpression unaryOperatorExpression) throws SemanticException {
				switch (unaryOperatorExpression.operator()) {
					case LOGICAL_NOT, NEGATION -> assertIsBoolean(unaryOperatorExpression.expression());
				}
				return Visitor.super.visit(unaryOperatorExpression);
			}

			@Override
			public Optional<Void> visit(SBinaryOperatorExpression binaryOperatorExpression) throws SemanticException {
				switch (binaryOperatorExpression.operator()) {
					case ASSIGN -> {
						SExprType rhsType = binaryOperatorExpression.rhs().type();
						SExprType lhsType = binaryOperatorExpression.lhs().type();
						if (!rhsType.isAssignableTo(lhsType)) {
							throw new SemanticException(source, null, "Assignment of incompatible type");
						}
					}
					case EQUAL, NOT_EQUAL -> {
						SExprType rhsType = binaryOperatorExpression.rhs().type();
						SExprType lhsType = binaryOperatorExpression.lhs().type();

						if (!lhsType.isAssignableTo(rhsType) && !rhsType.isAssignableTo(lhsType)) {
							throw new SemanticException(source, null, "Incompatible types in comparison");
						}
					}
					case LOGICAL_OR, LOGICAL_AND -> {
						assertIsBoolean(binaryOperatorExpression.lhs());
						assertIsBoolean(binaryOperatorExpression.rhs());
					}
					case LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL, ADD, SUBTRACT, MULTIPLY, DIVIDE,
						MODULO -> {
						assertIsInt(binaryOperatorExpression.lhs());
						assertIsInt(binaryOperatorExpression.rhs());
					}
				}
				return Visitor.super.visit(binaryOperatorExpression);
			}

			@Override
			public Optional<Void> visit(SSystemOutWriteExpression systemOutWriteExpression) throws SemanticException {
				assertIsInt(systemOutWriteExpression.argument());
				return Visitor.super.visit(systemOutWriteExpression);
			}

			@Override
			public Optional<Void> visit(SSystemOutPrinlnExpression systemOutPrinlnExpression) throws SemanticException {
				assertIsInt(systemOutPrinlnExpression.argument());
				return Visitor.super.visit(systemOutPrinlnExpression);
			}

			@Override
			public Optional<Void> visit(SMethodInvocationExpression methodInvocationExpression)
				throws SemanticException {

				SMethod target = methodInvocationExpression.method();
				List<LocalVariableDeclaration> parameters = target.parameters();
				List<SExpression> arguments = methodInvocationExpression.arguments();

				if (parameters.size() != arguments.size()) {
					throw new SemanticException(source, null, "Received wrong number of arguments");
				}

				for (int i = 0; i < parameters.size(); i++) {
					if (!arguments.get(i).type().isAssignableTo(parameters.get(i).getType())) {
						throw new SemanticException(source, null, "Mismatched types at index " + i);
					}
				}

				return Visitor.super.visit(methodInvocationExpression);
			}

			@Override
			public Optional<Void> visit(SReturnStatement returnStatement) throws SemanticException {
				if (currentMethod == null) {
					throw new IllegalStateException("Return outside of method");
				}
				Optional<SExpression> returnValue = returnStatement.returnValue();

				if (returnValue.isEmpty()) {
					if (returnStatement.returnValue().isPresent()) {
						throw new SemanticException(source, null, "Void method must not return anything");
					}
					return Visitor.super.visit(returnStatement);
				}

				if (!returnValue.get().type().isAssignableTo(currentMethod.returnType().asExprType())) {
					throw new SemanticException(source, null, "Not assignable to return type");
				}

				return Visitor.super.visit(returnStatement);
			}

			@Override
			public Optional<Void> visit(SNewObjectExpression newObjectExpression) throws SemanticException {
				Optional<SNormalType> normalType = newObjectExpression.type().asNormalType();

				if (normalType.isPresent() && normalType.get().arrayLevel() == 0) {
					if (normalType.get().basicType().asStringType().isPresent()) {
						throw new SemanticException(source, null, "Can not create instances of String");
					}
				}

				return Visitor.super.visit(newObjectExpression);
			}

			private void assertIsBoolean(SExpression expression) throws SemanticException {
				Optional<SNormalType> normalType = expression.type().asNormalType();

				if (normalType.isEmpty() || normalType.get().arrayLevel() != 0) {
					throw new SemanticException(source, null, "Condition must be a boolean");
				}

				if (normalType.get().basicType().asBooleanType().isEmpty()) {
					throw new SemanticException(source, null, "Condition must be a boolean");
				}
			}

			private void assertIsInt(SExpression expression) throws SemanticException {
				Optional<SNormalType> normalType = expression.type().asNormalType();

				if (normalType.isEmpty() || normalType.get().arrayLevel() != 0) {
					throw new SemanticException(source, null, "Expression must be an integer");
				}

				if (normalType.get().basicType().asIntType().isEmpty()) {
					throw new SemanticException(source, null, "Expression must be an integer");
				}
			}

		};

		for (SClassDeclaration declaration : classes.getAll()) {
			visitor.visit(declaration);
		}
	}

	void checkSideEffects(Namespace<SClassDeclaration> classes) throws SemanticException {
		// TODO Check if all ExpressionStatements have side effects

		Visitor<Void> visitor = new Visitor<>() {
			@Override
			public Optional<Void> visit(SExpressionStatement expressionStatement) throws SemanticException {
				SExpression expression = expressionStatement.expression();
				return switch (expression) {
					case SMethodInvocationExpression ignored -> Optional.empty();
					case SBinaryOperatorExpression op && op.operator() == BinaryOperator.ASSIGN -> Optional.empty();
					default -> throw new SemanticException(source, null,
						"Expression statement must habe side " + "effects");
				};
			}
		};

		for (SClassDeclaration declaration : classes.getAll()) {
			visitor.visit(declaration);
		}
	}

	void checkAssignments(Namespace<SClassDeclaration> classes) {
		// TODO Check if all assignments assign to an lvalue
	}

	void checkReturnPaths(Namespace<SClassDeclaration> classes) {
		// TODO Implement
	}

	SMethod findMainMethod(Namespace<SClassDeclaration> classes) throws SemanticException {
		// TODO Implement
		// TODO Check main method semantics

		Visitor<SMethod> visitor = new Visitor<>() {
			private SMethod foundMainMethod;
			private LocalVariableDeclaration mainMethodParameter;

			@Override
			public Optional<SMethod> visit(SMethod method) throws SemanticException {
				if (!method.isStatic()) {
					return Optional.empty();
				}
				if (this.foundMainMethod != null) {
					throw new SemanticException(source, null, "Found second main method!");
				}
				if (!method.name().ident().equals("main")) {
					throw new SemanticException(source, null, "Only 'main' is allowed for static method names");
				}
				if (method.parameters().size() != 1) {
					throw new IllegalArgumentException("The main method must have exactly one parameter");
				}
				LocalVariableDeclaration parameter = method.parameters().get(0);

				if (parameter.getType().arrayLevel() != 1) {
					throw new SemanticException(source, null, "The main method must have a String[] parameter");
				}
				if (parameter.getType().basicType().asStringType().isEmpty()) {
					throw new SemanticException(source, null, "The main method must have a String[] parameter");
				}

				this.foundMainMethod = method;
				this.mainMethodParameter = parameter;

				Visitor.super.visit(method);
				return Optional.of(method);
			}

			@Override
			public Optional<SMethod> visit(SLocalVariableExpression localVariableExpression) throws SemanticException {
				if (localVariableExpression.localVariable() == mainMethodParameter) {
					throw new SemanticException(source, null, "Usage of main method parameter is forbidden");
				}
				return Visitor.super.visit(localVariableExpression);
			}
		};

		Optional<SMethod> main = Optional.empty();
		for (SClassDeclaration declaration : classes.getAll()) {
			main = visitor.visit(declaration);
		}
		return main.orElseThrow(() -> new SemanticException(source, null, "Did not find a main method :/"));
	}
}
