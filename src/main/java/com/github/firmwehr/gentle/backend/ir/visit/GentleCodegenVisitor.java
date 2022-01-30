package com.github.firmwehr.gentle.backend.ir.visit;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaAdd;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaConst;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import firm.Graph;

import java.util.List;

public class GentleCodegenVisitor implements IkeaVisitor<Void> {

	private CodeBlock code;

	@Override
	public Void defaultReturnValue() {
		throw new InternalCompilerException("unreachable code became reachable, run!");
	}

	@Override
	public Void defaultVisit(IkeaNode node) {
		// throw new InternalCompilerException("Unexpected node found: " + node);
		return null;
	}

	public String visit(Graph graph, List<IkeaBløck> blocks) {
		String functionName = graph.getEntity().getLdName();

		StringBuilder source = new StringBuilder();
		for (IkeaBløck block : blocks) {
			code = new CodeBlock();
			code.addComment("start block " + block.id());
			block.accept(this);
			code.addComment("end block: " + block.id());
			source.append(code.code()).append("\n\n");
		}

		return source.toString();
	}

	@Override
	public Void visit(IkeaBløck block) {

		for (IkeaNode node : block.nodes()) {
			code.addComment("node: " + node);
			node.accept(this);
		}

		return null;
	}

	@Override
	public Void visit(IkeaConst node) {
		code.addOp("mov", node, "$" + node.value().asLong(), node.uncheckedRegister().nameForSize(node));
		return null;
	}

	@Override
	public Void visit(IkeaAdd node) {
		// TODO: consider abusing vmov for pushback of const

		code.addOp("add", node, "arg1, arg2");
		return null;
	}

	private static class CodeBlock {
		private final StringBuilder sb = new StringBuilder();

		public String code() {
			return sb.toString();
		}

		public void addComment(String comment) {
			addLine("// " + comment);
		}

		public void addOp(String op, IkeaBøx.IkeaRegisterSize size, String... arg) {
			addLine(op + size.getOldRegisterSuffix() + " " + String.join(", ", arg));
		}

		public void addOp(String op, IkeaNode node, String... arg) {
			addLine(op + node.size().getOldRegisterSuffix() + " " + String.join(", ", arg));
		}

		public void addLine(String line) {
			sb.append(line).append("\n");
		}
	}
}
