package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.debug.DebugInfoAllocate.AllocateElementType;
import com.github.firmwehr.gentle.debug.DebugInfoArrayAccessTarget.ArrayAccessTargetType;
import com.github.firmwehr.gentle.debug.DebugInfoCompare.CompareElementType;
import com.github.firmwehr.gentle.debug.DebugInfoCondToBoolBlock.CondToBoolBlockType;
import com.github.firmwehr.gentle.debug.DebugInfoFieldAccess.FieldAccessElementType;
import com.github.firmwehr.gentle.debug.DebugInfoIfBlock.IfBlockType;
import com.github.firmwehr.gentle.debug.DebugInfoMethodInvocation.MethodInvocationElementType;
import com.github.firmwehr.gentle.debug.DebugInfoShortCircuitBlock.ShortCircuitBlockType;
import com.github.firmwehr.gentle.debug.DebugInfoWhileBlock.WhileBlockType;
import com.github.firmwehr.gentle.semantic.ast.expression.SArrayAccessExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SBinaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SFieldAccessExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SMethodInvocationExpression;
import com.github.firmwehr.gentle.semantic.ast.statement.SIfStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SWhileStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record FirmNodeMetadata(
	List<IkeaNode> associatedBackendNodes,
	List<HasDebugInformation> semanticElements
) {

	public static FirmNodeMetadata forElement(HasDebugInformation source) {
		return new FirmNodeMetadata(new ArrayList<>(), new ArrayList<>(List.of(source)));
	}

	public static FirmNodeMetadata forIfBlock(SIfStatement statement, IfBlockType blockType) {
		return forElement(new DebugInfoIfBlock(statement, blockType));
	}

	public static FirmNodeMetadata forWhileBlock(SWhileStatement statement, WhileBlockType blockType) {
		return forElement(new DebugInfoWhileBlock(statement, blockType));
	}

	public static FirmNodeMetadata forCondToBoolBlock(HasDebugInformation source, CondToBoolBlockType blockType) {
		return forElement(new DebugInfoCondToBoolBlock(source, blockType));
	}

	public static FirmNodeMetadata forCondToBoolPhi(HasDebugInformation source) {
		return forElement(new DebugInfoCondToBoolPhi(source));
	}

	public static FirmNodeMetadata forArrayAccessTarget(SArrayAccessExpression source, ArrayAccessTargetType type) {
		return forElement(new DebugInfoArrayAccessTarget(source, type));
	}

	public static FirmNodeMetadata forShortCircuitBlock(SBinaryOperatorExpression source, ShortCircuitBlockType type) {
		return forElement(new DebugInfoShortCircuitBlock(source, type));
	}

	public static FirmNodeMetadata forCompare(SExpression source, CompareElementType type) {
		return forElement(new DebugInfoCompare(source, type));
	}

	public static FirmNodeMetadata forFieldLoad(SFieldAccessExpression source, FieldAccessElementType type) {
		return forElement(new DebugInfoFieldAccess(source, type));
	}

	public static FirmNodeMetadata forMethodInvocation(
		SMethodInvocationExpression source, MethodInvocationElementType type
	) {
		return forElement(new DebugInfoMethodInvocation(source, type));
	}

	public static FirmNodeMetadata forAllocate(SExpression source, AllocateElementType type) {
		return forElement(new DebugInfoAllocate(source, type));
	}

	@Override
	public String toString() {
		String result = "";

		if (!associatedBackendNodes.isEmpty()) {
			result += associatedBackendNodes.stream().map(Object::toString).collect(Collectors.joining(", "));
			result += "\n";
		}

		if (!semanticElements.isEmpty()) {
			result +=
				semanticElements.stream().map(HasDebugInformation::toDebugString).collect(Collectors.joining(", "));
		}

		return result;
	}
}
