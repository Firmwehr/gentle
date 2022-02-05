package com.github.firmwehr.gentle.backend.lego.visit;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.lego.LegoBøx.LegoRegisterSize;
import com.github.firmwehr.gentle.backend.lego.LegoParentBløck;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.codegen.RegisterTransferGraph;
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
import com.github.firmwehr.gentle.backend.lego.nodes.LegoSetcc;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoShift;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoShr;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoSpill;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoSub;
import com.github.firmwehr.gentle.backend.lego.register.X86Register;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.Pair;
import com.google.common.collect.Lists;
import firm.Graph;
import firm.MethodType;
import firm.Relation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.github.firmwehr.gentle.backend.lego.LegoBøx.LegoRegisterSize.BITS_32;
import static com.github.firmwehr.gentle.backend.lego.LegoBøx.LegoRegisterSize.BITS_64;

// TODO Rename to JättestorCodegenVisitor
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

		int paramCount = ((MethodType) graph.getEntity().getType()).getNParams();

		int stackFrameSize = blocks.stream()
			.flatMap(it -> it.nodes().stream())
			.filter(it -> it instanceof LegoSpill)
			.mapToInt(it -> (((LegoSpill) it).spillSlot() + 1) * 8)
			.max()
			.orElse(0);

		// pad stack frame to next 16 bytes for alignment
		if (stackFrameSize % 16 != 0) {
			// misaligned
			stackFrameSize = (stackFrameSize / 16) * 16 + 16;
		}

		StringBuilder source = new StringBuilder();
		source.append("/* function: ")
			.append(functionName)
			.append(", argument count: ")
			.append(paramCount)
			.append(" */\n");

		// prologue
		code = new CodeBlock();
		code.noSuffixOp(".globl", functionName);
		code.noSuffixOp(".type", functionName, "@function");
		code.label(functionName);

		code.noSuffixOp("push", "%rbp");
		code.noSuffixOp("movq", "%rsp", "%rbp");
		code.noSuffixOp("sub", "$" + stackFrameSize, "%rsp");

		source.append(code.code()).append("\n");

		for (LegoPlate block : blocks) {
			code = new CodeBlock();
			code.comment("start block " + block.id() + ":");

			code.label(labelForBlock(block));
			lowerSpilledPhis(block);
			lowerPhi(block);

			block.accept(this);
			code.comment("end block: " + block.id());
			source.append(code.code()).append("\n\n");
		}

		return source.toString();
	}

	private void lowerSpilledPhis(LegoPlate block) {
		List<LegoPhi> spilledPhis = new ArrayList<>();
		for (LegoNode node : block.nodes()) {
			if (!(node instanceof LegoPhi phi)) {
				continue;
			}
			if (phi.inputs().get(0) instanceof LegoSpill) {
				spilledPhis.add(phi);
			}
		}

		if (spilledPhis.isEmpty()) {
			return;
		}

		code.line("TODO: Cool spilled phi handling");
	}

	private void lowerPhi(LegoPlate block) {
		X86Register stackPointer = X86Register.RSP;
		String rsp = stackPointer.nameForSize(BITS_64);
		RegisterTransferGraph<X86Register> transferGraph = new RegisterTransferGraph<>(Set.of(X86Register.RSP));
		List<LegoPhi> phis = block.nodes()
			.stream()
			.filter(it -> it instanceof LegoPhi)
			.filter(it -> !(it.inputs().get(0) instanceof LegoSpill))
			.map(it -> (LegoPhi) it)
			.toList();

		for (LegoParentBløck parent : block.parents()) {
			for (LegoPhi phi : phis) {
				X86Register source = phi.parent(parent.parent()).uncheckedRegister();
				transferGraph.addMove(source, phi.uncheckedRegister());
			}
		}

		List<Pair<X86Register, X86Register>> moves = transferGraph.generateMoveSequence();

		if (moves.isEmpty()) {
			return;
		}

		code.comment("lower phi (spill stack pointer)");
		code.line("vmovd %s, %%xmm0".formatted(rsp));
		for (var pair : moves) {
			code.op("mov", BITS_64, pair.first().nameForSize(BITS_64), pair.second().nameForSize(BITS_64));
		}
		code.line("vmovd %%xmm0, %s".formatted(rsp));
	}

	public String labelForBlock(LegoPlate block) {
		return "block_" + block.id();
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
		code.comment("Argument count: " + call.inputs().size());

		if (call.inputs().size() % 2 != 0) {
			code.comment("aligning stack on 16 byte boundary");
			code.op("sub", BITS_64, "$8", "%rsp");
		}

		code.comment("pushing arguments");
		for (LegoNode input : Lists.reverse(call.inputs())) {
			code.line("push " + input.uncheckedRegister().nameForSize(BITS_64));
		}

		code.line("call " + call.entity().getLdName());

		int pushedArgumentsSize = call.inputs().size() * 8;
		if (call.inputs().size() % 2 != 0) {
			code.comment("re-added 8 padding bytes");
			pushedArgumentsSize += 8;
		}
		code.comment("restoring stack");
		code.op("add", BITS_64, "$" + pushedArgumentsSize, "%rsp");

		return null;
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
		LegoRegisterSize divSize;
		LegoNode left = div.inputs().get(0);
		if (left.uncheckedRegister() != X86Register.RAX) {
			code.op("mov", left, left.asRegisterName(), X86Register.RAX.nameForSize(left));
		}
		if (div.small()) {
			divSize = BITS_32;
			code.comment("small div possible, generating 32bit division (extending to 64bit)");
			code.line("cltd");
		} else {
			code.comment("both arguments are unknown, generating 64bit division (extending to 128bit)");
			code.line("cqto");
			divSize = BITS_64;
		}
		code.op("idiv", divSize, div.inputs().get(1).uncheckedRegister().nameForSize(divSize));
		return null;
	}

	@Override
	public Void visit(LegoSetcc legoSetcc) {
		String op = "set" + relationSuffix(legoSetcc.relation());
		String registerName = legoSetcc.uncheckedRegister().nameForSize(LegoRegisterSize.BITS_8);
		code.noSuffixOp(op, registerName);
		code.comment("clear upper bits, setcc only sets one byte");
		code.noSuffixOp("movsx", registerName, legoSetcc.uncheckedRegister().nameForSize(BITS_64));
		return null;
	}

	@Override
	public Void visit(LegoJcc jcc) {
		String op = "j" + relationSuffix(jcc.relation());
		code.comment("if true");
		code.noSuffixOp(op, labelForBlock(jcc.trueTarget()));
		code.comment("else false");
		code.noSuffixOp("jmp", labelForBlock(jcc.falseTarget()));
		return null;
	}

	private String relationSuffix(Relation relation) {
		return switch (relation) {
			case Equal -> "e";
			case Less -> "l";
			case Greater -> "g";
			case LessEqual -> "le";
			case GreaterEqual -> "ge";
			case LessGreater, UnorderedLessGreater -> "ne";
			default -> throw new InternalCompilerException(":( Where do we use " + relation + "⸘‽");
		};
	}

	@Override
	public Void visit(LegoJmp jmp) {
		code.noSuffixOp("jmp", labelForBlock(jmp.target()));
		return null;
	}

	@Override
	public Void visit(LegoMovLoad movLoad) {
		List<LegoNode> inputs = movLoad.inputs();
		code.op("mov", movLoad, "(%s)".formatted(inputs.get(0).asRegisterName()), movLoad.asRegisterName());
		return null;
	}

	@Override
	public Void visit(LegoMovLoadEx movLoadEx) {
		throw new InternalCompilerException("movLoadEx is not supported");
	}

	@Override
	public Void visit(LegoMovStoreEx movStoreEx) {
		throw new InternalCompilerException("movStoreEx is not supported");
	}

	@Override
	public Void visit(LegoMovStore movStore) {
		List<LegoNode> inputs = movStore.inputs();
		LegoNode address = inputs.get(0);
		LegoNode value = inputs.get(1);
		code.op("mov", value.size(), value.asRegisterName(), "(%s)".formatted(address.asRegisterName()));
		return null;
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
		code.comment("needs no code, has been lowered on block enter");
		return null;
	}

	@Override
	public Void visit(LegoSub sub) {
		LegoNode left = sub.left();
		LegoNode right = sub.right();
		if (right instanceof LegoImmediate imm) {
			moveToTarget(left, sub);
			code.op("sub", sub, imm(imm), sub.asRegisterName());
			return null;
		}
		X86Register a1Reg = right.uncheckedRegister();
		if (a1Reg == sub.uncheckedRegister()) {
			Set<X86Register> registers = X86Register.all();
			registers.remove(left.uncheckedRegister());
			registers.remove(right.uncheckedRegister());
			registers.remove(sub.uncheckedRegister());
			// TODO maybe choose randomly? Or the last one?
			a1Reg = registers.iterator().next();
			code.comment("always spill the full register, we don't know its content");
			code.line("vmovd %s, %%xmm0".formatted(a1Reg.nameForSize(BITS_64)));
			code.op("mov", right, right.asRegisterName(), a1Reg.nameForSize(right));
		}
		if (sub.uncheckedRegister() != left.uncheckedRegister()) {
			code.op("mov", sub, left.asRegisterName(), sub.asRegisterName());
		}
		code.op("sub", sub, a1Reg.nameForSize(left), sub.asRegisterName());
		if (a1Reg != right.uncheckedRegister()) {
			// always un-spill the full register, we don't know its content
			code.comment("always reload the full register, we don't know its content");
			code.line("vmovd %%xmm0, %s".formatted(a1Reg.nameForSize(BITS_64)));
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
		throw new InternalCompilerException("lego copy has been deprecated");
	}

	@Override
	public Void visit(LegoPerm perm) {
		throw new InternalCompilerException("backend is not supposed to generate perm");
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
	}

	@Override
	public Void visit(LegoAnd and) {
		visitCommutative(and, "and");
		return null;
	}

	@Override
	public Void visit(LegoMul legoMul) {
		visitCommutative(legoMul, "imul");
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

	@Override
	public Void visit(LegoProj node) {
		code.comment("Omitting codegen as proj is only register limitation");
		return null;
	}

	@Override
	public Void visit(LegoSpill node) {
		// well, we seem to be always going with the 8 byte sized slot
		var slotOffset = (node.spillSlot() + 1) * 8;
		code.op("mov", node, node.inputs().get(0).asRegisterName(), "-%d(%%rbp)".formatted(slotOffset));

		return null;
	}

	@Override
	public Void visit(LegoReload node) {
		// well, we seem to be always going with the 8 byte sized slot
		var slotOffset = (node.spillSlot() + 1) * 8;
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
		LegoNode input = node.inputs().get(0);

		if (node.size() == input.size()) {
			code.comment("Same size, moving...");
			code.op("mov", node.size(), input.asRegisterName(), node.asRegisterName());
			return null;
		}

		if (input.size() == BITS_64 && node.size() == BITS_32) {
			code.comment("Truncating 64 to 32 bit by using mov of lower 32 bit");
			String from = input.uncheckedRegister().nameForSize(BITS_32);
			code.op("mov", node, node.asRegisterName(), from);
			return null;
		}

		if (input.size() == BITS_32 && node.size() == BITS_64) {
			code.comment("Sign extending from 32 to 64 bit");
			code.noSuffixOp("movsxd", input.asRegisterName(), node.asRegisterName());
			return null;
		}

		throw new InternalCompilerException(
			"Unknown conversion. Tried to convert " + input.size() + " to " + node.size());
	}

	@Override
	public Void visit(LegoRet node) {
		code.comment("function epilogue, restore stackframe and previous frame's basepointer");
		code.line("mov %rbp, %rsp");
		code.line("pop %rbp");
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

		public void op(String op, LegoRegisterSize size, String... arg) {
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
