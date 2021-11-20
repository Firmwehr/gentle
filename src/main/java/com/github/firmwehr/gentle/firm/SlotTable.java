package com.github.firmwehr.gentle.firm;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.LinkedHashMap;
import java.util.Map;

public class SlotTable {
	private int counter;
	private final Map<LocalVariableDeclaration, Integer> toIndexMap = new LinkedHashMap<>();

	public SlotTable(SMethod method) {
		if (!method.isStatic()) {
			LocalVariableDeclaration thisDummy = createThisDummy(method.classDecl());
			this.toIndexMap.put(thisDummy, counter++);
		}
	}

	public int computeIndex(LocalVariableDeclaration localVariable) {
		return this.toIndexMap.computeIfAbsent(localVariable, ignored -> counter++);
	}

	private LocalVariableDeclaration createThisDummy(SClassDeclaration classDeclaration) {
		return new LocalVariableDeclaration(classDeclaration.type(), SourceSpan.dummy(), Ident.dummy("this"));
	}
}
