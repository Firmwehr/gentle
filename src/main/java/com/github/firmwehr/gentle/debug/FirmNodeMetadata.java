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
import com.github.firmwehr.gentle.source.Source;
import com.github.firmwehr.gentle.source.SourcePosition;
import com.github.firmwehr.gentle.source.SourceSpan;
import com.sun.jna.Pointer;
import firm.DebugInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
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

	public String toDebugInfoString(Source source) {
		String message = toDebugString(source);
		int line = findSourcePos(source).map(SourcePosition::line).orElse(-1);
		int col = findSourcePos(source).map(SourcePosition::column).orElse(-1);

		return message + " @ " + line + ":" + col;
	}

	public Pointer toDebugInfo(Source source) {
		String message = toDebugString(source);

		var maybeSourcePosition = findSourcePos(source);
		if (maybeSourcePosition.isEmpty()) {
			return DebugInfo.createInfo(message, -1, -1);
		}
		var sourcePosition = maybeSourcePosition.get();
		return DebugInfo.createInfo(message, sourcePosition.line(), sourcePosition.column());
	}

	private Optional<SourcePosition> findSourcePos(Source source) {
		for (var element : semanticElements) {
			var maybeSpan = element.debugSpan();
			if (maybeSpan.isEmpty()) {
				continue;
			}
			var span = maybeSpan.get();
			var sourcePosition = source.positionFromOffset(span.startOffset());
			return Optional.of(sourcePosition);
		}
		return Optional.empty();
	}

	private String toDebugString(Source source) {
		String result = "";

		if (!associatedBackendNodes.isEmpty()) {
			result += associatedBackendNodes.stream().map(Object::toString).collect(Collectors.joining(", "));
			result += "\n";
		}

		if (!semanticElements.isEmpty()) {
			StringJoiner parts = new StringJoiner("\n");
			for (HasDebugInformation element : semanticElements) {
				if (element.debugSpan().isPresent()) {
					SourceSpan span = element.debugSpan().get();
					parts.add(getMessageForSpan(source, span));
				} else if (!element.additionalInfo().isBlank()) {
					parts.add(element.additionalInfo());
				} else {
					parts.add("<none>");
				}
			}
			result += parts;
		}

		return result;
	}


	private static String getMessageForSpan(Source source, SourceSpan span) {
		return escapeComments(source.content().substring(span.startOffset(), span.endOffset())) + "\n";
	}

	private static String escapeComments(String input) {
		return input.replace("/*", "[").replace("*/", "]");
	}
}
