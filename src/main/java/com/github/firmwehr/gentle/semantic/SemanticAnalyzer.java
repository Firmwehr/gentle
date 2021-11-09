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

	private SNormalType normalTypeFromAstType(Namespace<SClassDeclaration> classes, Type type)
		throws SemanticException {

		SBasicType basicType = switch (type.basicType()) {
			case BooleanType t -> new SBooleanType();
			case IdentType t -> new SClassType(classes.get(t.name()));
			case IntType t -> new SIntType();
			case VoidType t -> throw new SemanticException(source, t.sourceSpan(), "void not allowed here");
		};

		return new SNormalType(basicType, type.arrayLevel());
	}

	private SVoidyType voidyTypeFromAstType(Namespace<SClassDeclaration> classes, Type type) throws SemanticException {
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
				SNormalType type = normalTypeFromAstType(classes, field.type());
				SField sField = new SField(sClassDecl, name, type);
				sClassDecl.fields().put(name, sField);
			}

			for (Method method : classDecl.methods()) {
				Ident name = method.name();
				SVoidyType returnType = voidyTypeFromAstType(classes, method.returnType());
				List<LocalVariableDeclaration> parameters = new ArrayList<>();
				for (Parameter parameter : method.parameters()) {
					SNormalType type = normalTypeFromAstType(classes, parameter.type());
					parameters.add(new LocalVariableDeclaration(type, Optional.of(parameter.name())));
				}
				SMethod sMethod = SMethod.newMethod(sClassDecl, name, returnType, parameters);
				sClassDecl.methods().put(name, sMethod);
			}

			for (MainMethod mainMethod : classDecl.mainMethods()) {
				Ident name = mainMethod.name();
				SNormalType paramType = normalTypeFromAstType(classes, mainMethod.parameter().type());
				Ident paramName = mainMethod.parameter().name();
				LocalVariableDeclaration parameter = new LocalVariableDeclaration(paramType, Optional.of(paramName));
				SMethod sMethod = SMethod.newMainMethod(sClassDecl, name, new SVoidType(), List.of(parameter));
				sClassDecl.methods().put(name, sMethod);
			}
		}
	}

	void addFunctionBodies(Namespace<SClassDeclaration> classes) {
		// TODO Implement
	}

	void checkTypes(Namespace<SClassDeclaration> classes) {
		// TODO Implement
		// TODO Don't forget the String type
	}

	void checkReturnPaths(Namespace<SClassDeclaration> classes) {
		// TODO Implement
	}

	SMethod findMainMethod(Namespace<SClassDeclaration> classes) {
		// TODO Implement
		// TODO Check main method semantics
		return null;
	}
}
