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
import com.github.firmwehr.gentle.semantic.analysis.MainMethodLookupVisitor;
import com.github.firmwehr.gentle.semantic.analysis.SideEffectVisitor;
import com.github.firmwehr.gentle.semantic.analysis.TypecheckVisitor;
import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SField;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.SProgram;
import com.github.firmwehr.gentle.semantic.ast.basictype.SBasicType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SBooleanType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SClassType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SIntType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidType;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidyType;
import com.github.firmwehr.gentle.source.Source;

import java.util.ArrayList;
import java.util.List;

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

	/**
	 * Checks that the program typechecks, i.e. all assignments and usages are well typed.
	 * <br>
	 * Note that this will not reject statements like "null = null", as the types in this statement match. The {@link
	 * #checkAssignments(Namespace)} method performs such checks-
	 *
	 * @param classes the classes to analyze
	 *
	 * @throws SemanticException if any type error is detected.
	 */
	void checkTypes(Namespace<SClassDeclaration> classes) throws SemanticException {
		Visitor<Void> visitor = new TypecheckVisitor(source);

		for (SClassDeclaration declaration : classes.getAll()) {
			visitor.visit(declaration);
		}
	}

	/**
	 * Checks that all expressions have side effects.
	 *
	 * @param classes the classes to analyze
	 *
	 * @throws SemanticException if any statement does not have a side effect
	 */
	void checkSideEffects(Namespace<SClassDeclaration> classes) throws SemanticException {
		Visitor<Void> visitor = new SideEffectVisitor(source);

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

	/**
	 * Finds the unique main method for all given classes, failing if it does not exist, multiple exist or the main
	 * method semantics are not respected:
	 * <ul>
	 *     <li>There is only one static method</li>
	 *     <li>The static method is named main</li>
	 *     <li>The static method has a {@code String[]} parameter</li>
	 *     <li>The static method returns void</li>
	 *     <li>The parameter of the static method is not used in the method</li>
	 * </ul>
	 *
	 * @param classes the classes to analyze
	 *
	 * @return the found, globally unique, main method
	 *
	 * @throws SemanticException if any of the above conditions is violated
	 */
	SMethod findMainMethod(Namespace<SClassDeclaration> classes) throws SemanticException {
		var visitor = new MainMethodLookupVisitor(source);

		for (SClassDeclaration declaration : classes.getAll()) {
			visitor.visit(declaration);
		}

		return visitor.getFoundMainMethod();
	}

}
