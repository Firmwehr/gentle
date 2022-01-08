package com.github.firmwehr.gentle.backend.ir.visit;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize;
import com.github.firmwehr.gentle.backend.ir.IkeaImmediate;
import com.github.firmwehr.gentle.backend.ir.IkeaVirtualRegister;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaAdd;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaArgNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaCall;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaCmp;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaConst;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaConv;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaDiv;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaJcc;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaJmp;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovLoad;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovRegister;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovStore;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMul;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNeg;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaRet;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaSub;
import firm.Entity;
import firm.Graph;
import firm.MethodType;
import firm.Mode;
import firm.TargetValue;

import java.util.List;
import java.util.OptionalInt;
import java.util.StringJoiner;

// just like the panda, this code is reeeeeeeally stupid
public class DjungelskogVisitor implements IkeaVisitor<String> {

	@Override
	public String defaultReturnValue() {
		return "";
	}

	@Override
	public String defaultVisit(IkeaNode node) {
		return node.getClass().getSimpleName();
	}

	private String readFromStackToTarget(IkeaBøx box, String target) {
		var maybe = stackIndex(box);
		if (maybe.isPresent()) {
			return "movq -" + maybe.getAsInt() + "(%rbp), " + target;
		}
		if (box instanceof IkeaImmediate immediate) {
			return "movq $" + immediate.assemblyName() + ", " + target;
		}
		throw new InternalCompilerException("Found weird box: " + box);
	}

	private String storeFromTargetToStack(IkeaBøx box, String source) {
		int index = stackIndex(box).orElseThrow(() -> new InternalCompilerException("Could not find ..." + box));

		return "movq " + source + ", -" + index + "(%rbp)";
	}

	private String fromLines(String... lines) {
		return String.join("\n", lines) + '\n';
	}

	private String boiler(String asmInstruction, IkeaBøx left, IkeaBøx right, IkeaBøx result) {
		// @formatter:off
		return fromLines(
			"// " + asmInstruction,
			readFromStackToTarget(left, "%r8"),
			readFromStackToTarget(right, "%r9"),
			asmInstruction + " %r8, %r9",
			storeFromTargetToStack(result, "%r9"),
			"" // this line left intentionally blank, do not print this!
		);
		// @formatter:on
	}

	@Override
	public String visit(IkeaAdd add) {
		return boiler("add", add.getLeft().box(), add.getRight().box(), add.box());
	}

	@Override
	public String visit(IkeaSub sub) {
		return boiler("sub", sub.getLeft().box(), sub.getRight().box(), sub.box());
	}

	@Override
	public String visit(IkeaRet ret) {
		String result = "";
		if (ret.getValue().isPresent()) {
			IkeaNode value = ret.getValue().get();
			result += readFromStackToTarget(value.box(), "%rax") + "\n";
		}
		result += "\nret";
		return result;
	}

	@Override
	public String visit(IkeaCall call) {
		StringBuilder result = new StringBuilder();

		for (IkeaNode argument : call.arguments()) {
			result.append(readFromStackToTarget(argument.box(), "%r8")).append("\n");
			result.append("pushq %r8").append("\n");
		}

		result.append("callq ").append(call.address().getEntity().getLdName()).append("\n");

		if (!isVoid(call.address().getEntity())) {
			result.append(storeFromTargetToStack(call.box(), "%rax")).append("\n");
		}

		//		result = """
		//			/* Setup arguments... */
		//			/* Save old stack pointer */
		//			pushq %rsp
		//			pushq (%rsp)
		//			/* Align stack to 16 bytes */
		//			andq $-0x10, %rsp
		//			CALL
		//			/* Restore old stack pointer */
		//			movq 8(%rsp), %rsp""".replace("CALL", result);

		return result.toString();
	}

	@Override
	public String visit(IkeaCmp cmp) {
		String result = "";
		result += readFromStackToTarget(cmp.getLeft().box(), "%r8") + "\n";
		result += readFromStackToTarget(cmp.getRight().box(), "%r9") + "\n";
		result += "cmp %r9, %r8";
		return result;
	}

	@Override
	public String visit(IkeaJcc jcc) {
		String result = switch (jcc.getRelation()) {
			case Equal -> "je";
			case Less -> "jl";
			case Greater -> "jg";
			case LessEqual -> "jle";
			case GreaterEqual -> "jge";
			case LessGreater, UnorderedLessGreater -> "jne";
			default -> throw new InternalCompilerException(":( Where do we use " + jcc.getRelation());
		};

		result += " " + blockMarker(jcc.getTrueTarget());
		result += "\n";
		result += "jmp " + blockMarker(jcc.getFalseTarget());

		return result;
	}

	@Override
	public String visit(IkeaJmp jmp) {
		return "jmp " + blockMarker(jmp.getTarget());
	}

	@Override
	public String visit(IkeaMul mul) {
		String result = "";
		result += readFromStackToTarget(mul.getLeft().box(), "%rdx") + "\n";
		result += readFromStackToTarget(mul.getRight().box(), "%r8") + "\n";
		result += "imul %r8d\n";
		result += storeFromTargetToStack(mul.box(), "%rax");

		return result;
	}

	@Override
	public String visit(IkeaMovLoad movLoad) {
		String oldSuffix = movLoad.getSize().getOldRegisterSuffix();
		String newSuffix = movLoad.getSize().getNewRegisterSuffix();
		String result = "";
		result += readFromStackToTarget(movLoad.getAddress().box(), "%r8") + "\n";
		result += "mov%s (%%r8), %%r9%s".formatted(oldSuffix, newSuffix) + "\n";
		result += storeFromTargetToStack(movLoad.box(), "%r9") + "\n";
		return result;
	}

	@Override
	public String visit(IkeaMovRegister movRegister) {
		String oldSuffix = movRegister.getSize().getOldRegisterSuffix();
		String newSuffix = movRegister.getSize().getNewRegisterSuffix();

		String result = "";
		result += readFromStackToTarget(movRegister.getSource(), "%r8") + "\n";
		result += "mov%s %%r8%s, %%r9%s".formatted(oldSuffix, newSuffix, newSuffix) + "\n";
		result += storeFromTargetToStack(movRegister.box(), "%r9") + "\n";

		return result;
	}

	@Override
	public String visit(IkeaMovStore movStore) {
		String oldSuffix = movStore.getSize().getOldRegisterSuffix();
		String newSuffix = movStore.getSize().getNewRegisterSuffix();

		String result = "";
		result += readFromStackToTarget(movStore.getValue().box(), "%r8") + "\n";
		result += readFromStackToTarget(movStore.getAddress().box(), "%r9") + "\n";
		result += "mov%s %%r8%s, (%%r9)".formatted(oldSuffix, newSuffix);

		return result;
	}

	@Override
	public String visit(IkeaNeg neg) {
		IkeaImmediate zero = new IkeaImmediate(new TargetValue(0, Mode.getIs()), IkeaRegisterSize.BITS_64);
		return boiler("sub", neg.getParent().box(), zero, neg.box());
	}

	@Override
	public String visit(IkeaConv conv) {
		IkeaRegisterSize target = conv.getTargetSize();
		IkeaRegisterSize source = conv.getSourceSize();

		String result = "";
		result += readFromStackToTarget(conv.getParent().box(), "%r8") + "\n";
		result += readFromStackToTarget(conv.box(), "%r9") + "\n";

		if (source.equals(target)) {
			result += "mov%s %%r8, %%r9".formatted(target.getOldRegisterSuffix());
			return result;
		}

		if (target == IkeaRegisterSize.BITS_32 && source == IkeaRegisterSize.BITS_64) {
			result += storeFromTargetToStack(conv.box(), "%r8");
			return result;
		}

		if (target != IkeaRegisterSize.BITS_64 || source != IkeaRegisterSize.BITS_32) {
			throw new InternalCompilerException("Can not convert from " + source + " -> " + target);
		}

		result += "movsxd %r8d, %r9";

		return result;
	}

	@Override
	public String visit(IkeaDiv div) {
		String result = "/* " + div.getNode().toString() + " */\n";
		result += readFromStackToTarget(div.getLeft().box(), "%r8") + "\n";
		result += readFromStackToTarget(div.getRight().box(), "%r9") + "\n";
		result += "movsxd %r8d, %rax\n";
		result += "movsxd %r9d, %rbx\n";
		result += "cqto\n";
		result += "idiv %rbx\n";
		result += storeFromTargetToStack(div.getBoxQuotient(), "%rax") + "\n";
		result += storeFromTargetToStack(div.getBoxMod(), "%rdx") + "\n";
		return result;
	}

	@Override
	public String visit(IkeaBløck block) {
		StringJoiner result = new StringJoiner("\n");

		for (IkeaNode node : block.nodes()) {
			if (node instanceof IkeaConst || node instanceof IkeaArgNode) {
				continue;
			}
			result.add(node.accept(this));
		}

		return blockMarker(block) + ":\n" + result.toString().indent(2);
	}

	public String visit(Graph graph, List<IkeaBløck> blocks) {
		int paramCount = ((MethodType) graph.getEntity().getType()).getNParams();
		String result = ".function " + graph.getEntity().getLdName();
		result += " " + paramCount;
		result += " " + (isVoid(graph.getEntity()) ? "0" : "1");

		StringJoiner statements = new StringJoiner("\n");
		for (IkeaBløck block : blocks) {
			if (block.nodes().isEmpty()) {
				continue;
			}
			statements.add(block.accept(this));
		}

		result += "\n" + statements.toString().indent(2);
		result += "\n.endfunction\n";

		return result;
	}

	private String blockMarker(IkeaBløck block) {
		return ".block" + block.id();
	}

	private boolean isVoid(Entity entity) {
		MethodType type = (MethodType) entity.getType();
		return type.getNRess() == 0 || type.getResType(0).getMode().equals(Mode.getANY());
	}

	private OptionalInt stackIndex(IkeaBøx box) {
		if (box instanceof IkeaVirtualRegister register) {
			return OptionalInt.of(register.num() * 8);
		}
		if (box instanceof IkeaImmediate) {
			return OptionalInt.empty();
		}
		throw new InternalCompilerException("Git: " + box);
	}
}
