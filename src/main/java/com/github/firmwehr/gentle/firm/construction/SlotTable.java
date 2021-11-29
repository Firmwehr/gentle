package com.github.firmwehr.gentle.firm.construction;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.expression.SLocalVariableExpression;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.HashMap;
import java.util.Map;

/**
 * Firm methods need to know how many local variables they contain and address them by index.
 * <br>
 * <p>This class counts the local variables of a method and manages the {@link LocalVariableDeclaration} -> firm local
 * variable index mapping.</p>
 */
public class SlotTable {
	private final Map<LocalVariableDeclaration, Integer> toIndexMap;

	/**
	 * Creates a new slot table for the local variables and parameters of a passed method.
	 * <p>
	 * The index for a local variable or parameter can be retried with {@link #computeIndex(LocalVariableDeclaration)}.
	 *
	 * @param method the method to build a local variable table for
	 *
	 * @return the built table
	 */
	public static SlotTable forMethod(SMethod method) {
		Map<LocalVariableDeclaration, Integer> map = new HashMap<>();
		if (!method.isStatic()) {
			LocalVariableDeclaration thisDummy = createThisDummy(method.classDecl());
			map.put(thisDummy, map.size());
		}
		// parameters
		for (LocalVariableDeclaration parameter : method.parameters()) {
			map.put(parameter, map.size());
		}
		// local variables in body
		Visitor<Void> visitor = new Visitor<>() {

			@Override
			public Void defaultReturnValue() {
				return null;
			}

			@Override
			public Void visit(SLocalVariableExpression localVariableExpression) {
				map.putIfAbsent(localVariableExpression.localVariable(), map.size());
				return defaultReturnValue();
			}
		};
		try {
			visitor.visit(method);
		} catch (SemanticException e) {
			throw new InternalCompilerException("received exception in infallible visitor", e);
		}
		return new SlotTable(map);
	}

	private SlotTable(Map<LocalVariableDeclaration, Integer> map) {
		this.toIndexMap = map;
	}

	public int computeIndex(LocalVariableDeclaration localVariable) {
		return this.toIndexMap.computeIfAbsent(localVariable, var -> {
			throw new InternalCompilerException("encountered unknown variable " + var);
		});
	}

	public int size() {
		return this.toIndexMap.size();
	}

	private static LocalVariableDeclaration createThisDummy(SClassDeclaration classDeclaration) {
		return new LocalVariableDeclaration(classDeclaration.type(), SourceSpan.dummy(), Ident.dummy("this"));
	}
}
