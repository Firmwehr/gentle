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
import com.github.firmwehr.gentle.debug.DebugStore;
import com.google.common.collect.Lists;
import firm.Entity;
import firm.Graph;
import firm.MethodType;
import firm.Mode;
import firm.nodes.Node;

import java.util.List;
import java.util.OptionalInt;
import java.util.StringJoiner;

// just like the panda, this code is reeeeeeeally stupid
public class DjungelskogVisitor implements IkeaVisitor<String> {

	private final DebugStore debugStore;

	private String currentReturnLabel;

	public DjungelskogVisitor(DebugStore debugStore) {
		this.debugStore = debugStore;
	}

	@Override
	public String defaultReturnValue() {
		return "";
	}

	@Override
	public String defaultVisit(IkeaNode node) {
		throw new InternalCompilerException("Unexpected node found: " + node);
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

	private String simpleBinaryOperator(String asmInstruction, IkeaBøx left, IkeaBøx right, IkeaBøx result) {
		// @formatter:off
		return fromLines(
			"// " + asmInstruction,
			readFromStackToTarget(left, "%r8"),
			readFromStackToTarget(right, "%r9"),
			asmInstruction + " %%r8%s, %%r9%s".formatted(getRegisterSuffix(left), getRegisterSuffix(right)),
			storeFromTargetToStack(result, "%r9"),
			"" // this line left intentionally blank, do not print this!
		);
		// @formatter:on
	}

	@Override
	public String visit(IkeaAdd add) {
		return simpleBinaryOperator("add", add.getRight().box(), add.getLeft().box(), add.box());
	}

	@Override
	public String visit(IkeaSub sub) {
		return simpleBinaryOperator("sub", sub.getRight().box(), sub.getLeft().box(), sub.box());
	}

	@Override
	public String visit(IkeaRet ret) {
		String result = "";
		if (ret.getValue().isPresent()) {
			IkeaNode value = ret.getValue().get();
			result += readFromStackToTarget(value.box(), "%rax") + "\n";
		}
		result += "jmp %s".formatted(currentReturnLabel);
		return result;
	}

	@Override
	public String visit(IkeaCall call) {
		StringBuilder result = new StringBuilder();
		result.append("// ").append(call.address().getEntity().getLdName()).append("\n");

		// uneven number of argument requires additional push for 16 byte alignment
		int padding = 0;
		if (call.arguments().size() % 2 != 0) {
			result.append("sub $8, %rsp # padding").append("\n");
			padding = 8;
		}

		// cdecl requires arguments to be pushed in reverse, so they are correctly ordered when reading them again
		for (IkeaNode argument : Lists.reverse(call.arguments())) {
			result.append(readFromStackToTarget(argument.box(), "%r8")).append("\n");
			result.append("pushq %r8").append("\n");
		}

		result.append("callq ").append(call.address().getEntity().getLdName()).append("\n");

		// undo parameter pushs
		result.append("add $%s, %%rsp".formatted(call.arguments().size() * 8 + padding)).append("\n");

		if (!isVoid(call.address().getEntity())) {
			result.append(storeFromTargetToStack(call.box(), "%rax")).append("\n");
		}

		return result.toString();
	}

	@Override
	public String visit(IkeaCmp cmp) {
		String result = "";
		result += readFromStackToTarget(cmp.getLeft().box(), "%r8") + "\n";
		result += readFromStackToTarget(cmp.getRight().box(), "%r9") + "\n";
		result += "cmp %%r9%s, %%r8%s".formatted(getRegisterSuffix(cmp.getLeft().box()),
			getRegisterSuffix(cmp.getRight().box()));
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
		result += readFromStackToTarget(mul.getRight().box(), "%rax") + "\n";
		result += readFromStackToTarget(mul.getLeft().box(), "%r8") + "\n";
		result += "imul %%r8%s\n".formatted(getRegisterSuffix(mul.box()));
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
		String result = "";
		result += readFromStackToTarget(neg.getParent().box(), "%r8") + "\n";
		result += "neg %%r8%s\n".formatted(getRegisterSuffix(neg.box()));
		result += storeFromTargetToStack(neg.box(), "%r8") + "\n";
		return result;
	}

	@Override
	public String visit(IkeaConv conv) {
		IkeaRegisterSize target = conv.getTargetSize();
		IkeaRegisterSize source = conv.getSourceSize();

		String result = "";
		result += readFromStackToTarget(conv.getParent().box(), "%r8") + "\n";
		result += readFromStackToTarget(conv.box(), "%r9") + "\n";

		if (source.equals(target)) {
			result += storeFromTargetToStack(conv.box(), "%r8");
			return result;
		}

		if (target == IkeaRegisterSize.BITS_32 && source == IkeaRegisterSize.BITS_64) {
			result += storeFromTargetToStack(conv.box(), "%r8");
			return result;
		}

		if (target != IkeaRegisterSize.BITS_64 || source != IkeaRegisterSize.BITS_32) {
			throw new InternalCompilerException("Can not convert from " + source + " -> " + target);
		}

		result += "movsxd %r8d, %r9\n";
		result += storeFromTargetToStack(conv.box(), "%r9");

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
		debugStore.getMetadataString(block.origin()).ifPresent(it -> result.add("/* " + it + " */"));

		for (IkeaNode node : block.nodes()) {
			if (node instanceof IkeaConst || node instanceof IkeaArgNode) {
				continue;
			}
			for (Node underlyingFirmNode : node.getUnderlyingFirmNodes()) {
				debugStore.getMetadataString(underlyingFirmNode)
					.ifPresent(string -> result.add("/* " + string + " */"));
			}
			result.add(node.accept(this));
		}

		return blockMarker(block) + ":\n" + result.toString().indent(2);
	}

	public String visit(Graph graph, List<IkeaBløck> blocks) {
		String functionName = graph.getEntity().getLdName();
		currentReturnLabel = "%s____________return_block_of_this_function".formatted(functionName);

		int paramCount = ((MethodType) graph.getEntity().getType()).getNParams();

		int stackFrameSize = blocks.stream()
			.flatMap(it -> it.nodes().stream())
			.map(IkeaNode::box)
			.filter(it -> it instanceof IkeaVirtualRegister)
			.mapToInt(it -> stackIndex(it).orElseThrow())
			.max()
			.orElse(0);

		// pad stack frame to next 16 bytes for alignment
		if (stackFrameSize % 16 != 0) {
			// misaligned
			stackFrameSize = (stackFrameSize / 16) * 16 + 16;
		}

		String debugInfo = "// " + functionName + " with " + paramCount + " params";
		// store caller base pointer and move stack pointer to base pointer
		String result = """
			%s
			.globl	%s
			.type	%s, @function

			%s:
			// Function prologue, call address is above us pushed by "callq"
			push %%rbp
			movq %%rsp, %%rbp
			sub $%s, %%rsp
			""".formatted(debugInfo, functionName, functionName, functionName, stackFrameSize);

		// load arguments from caller stack into our stack (for each argument)
		for (int i = 0; i < paramCount; i++) {
			int loadOffset = 16 + i * 8;
			int storeOffset = 8 * (i + 1);
			result += "movq " + loadOffset + "(%rbp), %r8\n";
			result += "movq %r8, -" + storeOffset + "(%rbp)\n";
		}

		StringJoiner statements = new StringJoiner("\n");
		for (IkeaBløck block : blocks) {
			if (block.nodes().isEmpty()) {
				continue;
			}
			statements.add(block.accept(this));
		}

		result += "\n" + statements.toString().indent(2);
		result += """
			%s:
			mov	%%rbp, %%rsp
			pop	%%rbp
			ret
			""".formatted(currentReturnLabel);

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
			return OptionalInt.of((register.num() + 1) * 8);
		}
		if (box instanceof IkeaImmediate) {
			return OptionalInt.empty();
		}
		throw new InternalCompilerException("Git: " + box);
	}

	private String getRegisterSuffix(IkeaBøx box) {
		if (box instanceof IkeaVirtualRegister register) {
			return register.size().getNewRegisterSuffix();
		}
		if (box instanceof IkeaImmediate immediate) {
			return immediate.size().getNewRegisterSuffix();
		}
		throw new InternalCompilerException("Git: " + box);
	}
}
