package com.github.firmwehr.gentle.backend.lego.visit;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoAdd;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoConst;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoConv;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovRegister;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMul;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoProj;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoReload;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoRet;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoSpill;
import com.github.firmwehr.gentle.output.Logger;
import firm.Graph;

import java.util.List;

public class GentleCodegenVisitor implements LegoVisitor<Void> {

	private static final Logger LOGGER = new Logger(GentleCodegenVisitor.class, Logger.LogLevel.DEBUG);

	private CodeBlock code;

	@Override
	public Void defaultReturnValue() {
		throw new InternalCompilerException("unreachable code became reachable, run!");
	}

	@Override
	public Void defaultVisit(LegoNode node) {
		// throw new InternalCompilerException("Unexpected node found: " + node);
		return null;
	}

	public String visit(Graph graph, List<LegoPlate> blocks) {
		String functionName = graph.getEntity().getLdName();

		StringBuilder source = new StringBuilder();
		source.append("// function: ").append(functionName).append('\n');
		for (LegoPlate block : blocks) {
			code = new CodeBlock();
			code.comment("start block " + block.id() + ":");
			code.line("block_" + block.id());
			block.accept(this);
			code.comment("end block: " + block.id());
			source.append(code.code()).append("\n\n");
		}

		return source.toString();
	}

	@Override
	public Void visit(LegoPlate block) {

		for (LegoNode node : block.nodes()) {
			code.comment("node: " + node);
			node.accept(this);
		}

		return null;
	}

	@Override
	public Void visit(LegoConst legoConst) {
		code.op("mov", legoConst, "$" + legoConst.value().asLong(), legoConst.asRegisterName());
		return null;
	}

	@Override
	public Void visit(LegoAdd node) {
		var other = convert2AddressCode(node, node.inputs().get(0), node.inputs().get(1));
		code.op("add", node, other.uncheckedRegister().nameForSize(node), node.asRegisterName());

		return null;
	}

	@Override
	public Void visit(LegoMul node) {
		var other = convert2AddressCode(node, node.inputs().get(0), node.inputs().get(1));
		code.op("add", node, other.uncheckedRegister().nameForSize(node), node.asRegisterName());

		return null;
	}

	// TODO: Pretty sure we are going to nuke this method from orbit eventually, no need to document
	private LegoNode convert2AddressCode(LegoNode target, LegoNode n0, LegoNode n1) {
		var r = target.uncheckedRegister();

		// return whichever register is not target (or copy one operant into target and return other)
		if (r == n0.uncheckedRegister()) {
			return n1;
		} else if (r == n1.uncheckedRegister()) {
			return n0;
		} else {
			var r0 = n0.uncheckedRegister();
			var r1 = n1.uncheckedRegister();

			// destination register is not in operants, need to duplicate one arg in r
			code.comment(
				"both operants (%s, %s) survive, need to duplicate into target register %s".formatted(r0, r1, r));

			// we always copy r0 into r and return r1
			code.op("mov", target, n0.uncheckedRegister().nameForSize(target), target.asRegisterName());
			return n1;
		}
	}

	@Override
	public Void visit(LegoProj node) {
		code.comment("no code required"); // TODO: unless div

		return null;
	}

	@Override
	public Void visit(LegoSpill node) {
		// well, we seem to be always going with the 8 byte sized slot
		var slotOffset = node.spillSlot() * 8;
		code.op("mov", node, node.inputs().get(0).asRegisterName(), "-%d(%%rbp)".formatted(slotOffset));

		return null;
	}

	@Override
	public Void visit(LegoReload node) {
		// well, we seem to be always going with the 8 byte sized slot
		var slotOffset = node.spillSlot() * 8;
		code.op("mov", node, "-%d(%%rbp)".formatted(slotOffset), node.asRegisterName());

		return null;
	}

	@Override
	public Void visit(LegoMovRegister node) {
		var source = node.inputs().get(0);
		code.op("mov", node, source.uncheckedRegister().nameForSize(node), node.asRegisterName());

		return null;
	}

	@Override
	public Void visit(LegoConv node) {
		code.comment(" TODO: this is super complicated to do right and DjungelskogVisitor might be doing it wrong");

		return null;
	}

	@Override
	public Void visit(LegoRet node) {
		code.line("ret");

		return null;
	}

	private static class CodeBlock {
		private final StringBuilder sb = new StringBuilder();

		public String code() {
			return sb.toString();
		}

		public void comment(String comment) {
			line("// " + comment);
		}

		public void op(String op, LegoBøx.LegoRegisterSize size, String... arg) {
			line(op + size.getOldRegisterSuffix() + " " + String.join(", ", arg));
		}

		public void op(String op, LegoNode node, String... arg) {
			line(op + node.size().getOldRegisterSuffix() + " " + String.join(", ", arg));
		}

		public void line(String line) {
			sb.append(line).append("\n");
		}
	}
}
