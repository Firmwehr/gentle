package com.github.firmwehr.gentle.backend.lego.visit;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoAdd;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoAnd;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoArgNode;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoBinaryOp;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoCall;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoCmp;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoConst;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoConv;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoCopy;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoDiv;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoImmediate;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoJcc;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoJmp;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovLoad;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovLoadEx;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovRegister;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovStore;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovStoreEx;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMul;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNeg;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoPerm;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoPhi;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoProj;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoReload;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoRet;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoSal;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoSar;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoShift;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoShr;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoSpill;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoSub;
import com.github.firmwehr.gentle.backend.lego.register.X86Register;
import com.github.firmwehr.gentle.output.Logger;
import firm.Graph;

import java.util.List;
import java.util.Set;

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
		source.append("/* function: ").append(functionName).append(" */\n");
		for (LegoPlate block : blocks) {
			code = new CodeBlock();
			code.comment("start block " + block.id() + ":");
			code.label("block_" + block.id());
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
	public Void visit(LegoCall call) {
		return LegoVisitor.super.visit(call);
	}

	@Override
	public Void visit(LegoCmp cmp) {
		LegoNode a0 = cmp.left();
		LegoNode a1 = cmp.right();
		// LegoCmp has no direct out register and therefore a register size of ILLEGAL
		// => we use a0 for size
		if (a1 instanceof LegoImmediate imm) {
			code.op("cmp", a0, imm(imm), a0.asRegisterName());
		} else {
			code.op("cmp", a0, a1.asRegisterName(), a0.asRegisterName());
		}
		return null;
	}

	@Override
	public Void visit(LegoDiv div) {
		return LegoVisitor.super.visit(div);
	}

	@Override
	public Void visit(LegoJcc jcc) {
		String op = switch (jcc.relation()) {
			case Equal -> "je";
			case Less -> "jl";
			case Greater -> "jg";
			case LessEqual -> "jle";
			case GreaterEqual -> "jge";
			case LessGreater, UnorderedLessGreater -> "jne";
			default -> throw new InternalCompilerException(":( Where do we use " + jcc.relation());
		};
		// FIXME
		code.noSuffixOp(op, "TODO TODO TODO TODO");
		return null;
	}

	@Override
	public Void visit(LegoJmp jmp) {
		return LegoVisitor.super.visit(jmp);
	}

	@Override
	public Void visit(LegoMovLoad movLoad) {
		List<LegoNode> inputs = movLoad.inputs();
		code.op("mov", movLoad, "(%s)".formatted(inputs.get(0).asRegisterName()), movLoad.asRegisterName());
		return null;
	}

	@Override
	public Void visit(LegoMovLoadEx movLoadEx) {
		return LegoVisitor.super.visit(movLoadEx);
	}

	@Override
	public Void visit(LegoMovStore movStore) {
		List<LegoNode> inputs = movStore.inputs();
		LegoNode address = inputs.get(0);
		LegoNode value = inputs.get(1);
		code.op("mov", value.size(), value.asRegisterName(), "(%s)".formatted(address.asRegisterName()));
		return LegoVisitor.super.visit(movStore);
	}

	@Override
	public Void visit(LegoMovStoreEx movStoreEx) {
		return LegoVisitor.super.visit(movStoreEx);
	}

	@Override
	public Void visit(LegoNeg neg) {
		LegoNode legoNode = neg.inputs().get(0);
		if (legoNode.uncheckedRegister() != neg.uncheckedRegister()) {
			code.op("mov", legoNode, legoNode.asRegisterName(), neg.asRegisterName());
		}
		code.op("neg", neg, neg.asRegisterName());
		return null;
	}

	@Override
	public Void visit(LegoPhi phi) {
		return LegoVisitor.super.visit(phi);
	}

	@Override
	public Void visit(LegoSub sub) {
		List<LegoNode> inputs = sub.inputs();
		LegoNode a0 = inputs.get(0);
		LegoNode a1 = inputs.get(1);
		X86Register a1Reg = a1.uncheckedRegister();
		if (a1Reg == sub.uncheckedRegister()) {
			Set<X86Register> registers = X86Register.all();
			registers.remove(a0.uncheckedRegister());
			registers.remove(a1.uncheckedRegister());
			registers.remove(sub.uncheckedRegister());
			a1Reg = registers.iterator().next();
			code.comment("always spill the full register, we don't know its content");
			code.line("vmovd %s, %%xmm0".formatted(a1Reg.nameForSize(LegoBøx.LegoRegisterSize.BITS_64)));
			code.op("mov", a1, a1.asRegisterName(), a1Reg.nameForSize(a1));
		}
		if (sub.uncheckedRegister() != a0.uncheckedRegister()) {
			code.op("mov", sub, a0.asRegisterName(), sub.asRegisterName());
		}
		code.op("sub", sub, a1Reg.nameForSize(a0), sub.asRegisterName());
		if (a1Reg != a1.uncheckedRegister()) {
			// always un-spill the full register, we don't know its content
			code.comment("always reload the full register, we don't know its content");
			code.line("vmovd %%xmm0, %s".formatted(a1Reg.nameForSize(LegoBøx.LegoRegisterSize.BITS_64)));
		}
		return null;
	}

	@Override
	public Void visit(LegoSal sal) {
		return generateShift("sal", sal);
	}

	@Override
	public Void visit(LegoShr shr) {
		return generateShift("shr", shr);
	}

	@Override
	public Void visit(LegoSar sar) {
		return generateShift("sar", sar);
	}

	private Void generateShift(String mnemonic, LegoShift shift) {
		List<LegoNode> inputs = shift.inputs();
		LegoNode a0 = inputs.get(0);
		if (a0.uncheckedRegister() != shift.uncheckedRegister()) {
			code.op("mov", a0, a0.asRegisterName(), shift.asRegisterName());
		}
		code.op(mnemonic, shift, imm(shift.shiftValue()), shift.asRegisterName());
		return null;
	}

	@Override
	public Void visit(LegoCopy copy) {
		return LegoVisitor.super.visit(copy);
	}

	@Override
	public Void visit(LegoPerm perm) {
		return LegoVisitor.super.visit(perm);
	}

	@Override
	public Void visit(LegoArgNode argNode) {
		if (argNode.isPassedInRegister()) {
			code.comment("passed via register");
			return null; // no need to load from stack
		}
		int offset = argNode.stackOffset();
		code.op("mov", argNode, offset + "(%rbp)", argNode.asRegisterName());
		return null;
	}

	@Override
	public Void visit(LegoConst legoConst) {
		code.op("mov", legoConst, "$" + legoConst.value().asLong(), legoConst.asRegisterName());
		return null;
	}

	@Override
	public Void visit(LegoAdd legoAdd) {
		LegoNode left = legoAdd.left();
		LegoNode right = legoAdd.right();
		if (left.register().isPresent() && right.register().isPresent() &&
			left.uncheckedRegister() != right.uncheckedRegister() &&
			left.uncheckedRegister() != legoAdd.uncheckedRegister() &&
			right.uncheckedRegister() != legoAdd.uncheckedRegister()) {
			// if all registers are different, we can still use lea to avoid extra mov
			code.op("lea", legoAdd, "(%s, %s)".formatted(left.asRegisterName(), right.asRegisterName()),
				legoAdd.asRegisterName());
			return null;
		}
		// otherwise, we just go the common way for all commutative operations
		visitCommutative(legoAdd, "add");
		return null;
/*		if (right instanceof LegoImmediate imm) {
			moveToTarget(left, legoAdd);
			code.op("add", legoAdd, imm(imm), legoAdd.asRegisterName());
			return null;
		}
		X86Register targetRegister = legoAdd.uncheckedRegister();
		if (right.uncheckedRegister() != targetRegister && left.uncheckedRegister() != targetRegister) {
			// we have the same register for both the right argument and the target
			// but this can't be expressed in 2 address
			code.op("lea", legoAdd, "(%s, %s)".formatted(left.asRegisterName(), right.asRegisterName()),
				legoAdd.asRegisterName());
			return null;
		}
		if (left.uncheckedRegister() != targetRegister) {
			// a0 + a1 = target => add a1, a0 => move a0 to target if necessary
			code.op("mov", left, left.asRegisterName(), legoAdd.asRegisterName());
		}
		code.op("add", legoAdd, right.asRegisterName(), legoAdd.asRegisterName());
			*//*var other = convert2AddressCode(legoAdd, a0, a1);
			code.op("add", legoAdd, other.uncheckedRegister().nameForSize(legoAdd), legoAdd.asRegisterName());*//*
		return null;*/
	}

	@Override
	public Void visit(LegoAnd and) {
		visitCommutative(and, "and");
		return null;
	}

	@Override
	public Void visit(LegoMul legoMul) {
		visitCommutative(legoMul, "mul");
		return null;
	}

	private void moveToTarget(LegoNode argument, LegoNode target) {
		if (argument.uncheckedRegister() != target.uncheckedRegister()) {
			code.op("mov", argument, argument.asRegisterName(), target.asRegisterName());
		}
	}

	private void visitCommutative(LegoBinaryOp binaryOp, String mnemonic) {
		LegoNode left = binaryOp.left();
		LegoNode right = binaryOp.right();
		assert !(left instanceof LegoImmediate); // only one value is allowed to be directly encoded
		if (right instanceof LegoImmediate imm) {
			// create a mov for the left value if not already in target register
			moveToTarget(left, binaryOp);
			// encode the immediate value in the instruction
			code.op(mnemonic, binaryOp, imm(imm), binaryOp.asRegisterName());
			return;
		}
		// we have two source registers, now we need to deal with them
		X86Register targetRegister = binaryOp.uncheckedRegister();
		if (right.uncheckedRegister() == targetRegister && left.uncheckedRegister() != targetRegister) {
			// we have the same register for both the right argument and the target
			// but this can't be expressed in 2 address if the left isn't the same too.
			// However, we can swap left and right as the operation is commutative
			code.op(mnemonic, binaryOp, left.asRegisterName(), right.asRegisterName());
			return;
		}
		if (left.uncheckedRegister() != targetRegister) {
			// left + right = target => add right, left => move left to target if necessary
			code.op("mov", left, left.asRegisterName(), binaryOp.asRegisterName());
		}
		// if we are here, the left value should already be in the target register
		code.op(mnemonic, binaryOp, right.asRegisterName(), binaryOp.asRegisterName());
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

	public String imm(LegoConst legoConst) {
		return "$" + legoConst.value().asLong();
	}

	private String imm(LegoImmediate imm) {
		return "$" + imm.targetValue().asLong();
	}

	private static class CodeBlock {
		private final StringBuilder sb = new StringBuilder();

		public String code() {
			return sb.toString();
		}

		public void comment(String comment) {
			line("/* " + comment + " */");
		}

		public void op(String op, LegoBøx.LegoRegisterSize size, String... arg) {
			line(op + size.getOldRegisterSuffix() + " " + String.join(", ", arg));
		}

		public void op(String op, LegoNode node, String... arg) {
			line(op + node.size().getOldRegisterSuffix() + " " + String.join(", ", arg));
		}

		public void noSuffixOp(String op, String... arg) {
			line(op + " " + String.join(", ", arg));
		}

		public void label(String label) {
			line(label + ":");
		}

		public void line(String line) {
			sb.append(line).append("\n");
		}
	}
}
