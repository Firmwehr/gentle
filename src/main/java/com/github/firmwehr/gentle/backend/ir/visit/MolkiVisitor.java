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
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMod;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovLoad;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovStore;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMul;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNeg;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPhi;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaRet;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaSub;
import firm.Entity;
import firm.Graph;
import firm.MethodType;
import firm.Mode;

import java.util.List;
import java.util.StringJoiner;

import static java.util.stream.Collectors.joining;

public class MolkiVisitor implements IkeaVisitor<String> {

	@Override
	public String defaultReturnValue() {
		return "";
	}

	@Override
	public String defaultVisit(IkeaNode node) {
		return node.getClass().getSimpleName();
	}

	@Override
	public String visit(IkeaAdd add) {
		String left = reg(add.getLeft().box());
		String right = reg(add.getRight().box());
		String res = reg(add.box());
		return "add [ %s | %s ] -> %s".formatted(left, right, res);
	}

	@Override
	public String visit(IkeaPhi phi) {
		return "mov";
	}

	@Override
	public String visit(IkeaSub sub) {
		String left = reg(sub.getLeft().box());
		String right = reg(sub.getRight().box());
		String res = reg(sub.box());
		return "sub [ %s | %s ] -> %s".formatted(right, left, res);
	}

	@Override
	public String visit(IkeaRet ret) {
		String result = "";
		if (ret.getValue().isPresent()) {
			result += "mov %s, %%@r0".formatted(reg(ret.getValue().get().box()).replaceAll("[a-z]", ""));
		}
		result += "\nreturn";
		return result;
	}

	@Override
	public String visit(IkeaCall call) {
		String result = "call %s".formatted(call.address().getEntity().getLdName()) + " ";
		result += call.arguments()
			.stream()
			.map(it -> reg(it.box()))
			.map(it -> it.replaceAll("[a-z]", ""))
			.collect(joining(" | ", "[ ", " " + "]"));

		if (!isVoid(call.address().getEntity())) {
			result += " -> " + reg(call.box());
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

		return result;
	}

	@Override
	public String visit(IkeaCmp cmp) {
		return "cmp %s, %s".formatted(reg(cmp.getRight().box()), reg(cmp.getLeft().box()));
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
		return "mul [ %s | %s ] -> %s".formatted(reg(mul.getLeft().box()), reg(mul.getRight().box()), reg(mul.box()));
	}

	@Override
	public String visit(IkeaMovLoad movLoad) {
		String suffix = movLoad.getSize().getOldRegisterSuffix();
		return "mov%s (%s), %s".formatted(suffix, reg(movLoad.getAddress().box()), reg(movLoad.box()));
	}

	@Override
	public String visit(IkeaMovStore movStore) {
		String suffix = movStore.getSize().getOldRegisterSuffix();
		return "mov%s %s, (%s)".formatted(suffix, reg(movStore.getValue().box()), reg(movStore.getAddress().box()));
	}

	@Override
	public String visit(IkeaNeg neg) {
		return "neg %s".formatted(reg(neg.box()));
	}

	@Override
	public String visit(IkeaConv conv) {
		IkeaRegisterSize target = conv.getTargetSize();
		IkeaRegisterSize source = conv.getSourceSize();
		String fromReg = reg(conv.getParent().box());
		String toReg = reg(conv.box());

		if (source.equals(target)) {
			return "mov%s %s, %s".formatted(target.getOldRegisterSuffix(), fromReg, toReg);
		}

		if (target == IkeaRegisterSize.BITS_32 && source == IkeaRegisterSize.BITS_64) {
			String adjustedFrom = fromReg + target.getNewRegisterSuffix();
			return "/* Cast */\nmov%s %s, %s".formatted(target.getOldRegisterSuffix(), adjustedFrom, toReg);
		}

		if (target != IkeaRegisterSize.BITS_64 || source != IkeaRegisterSize.BITS_32) {
			throw new InternalCompilerException("Can not convert from " + source + " -> " + target);
		}

		return "movsxd %s, %s".formatted(fromReg, toReg);
	}

	@Override
	public String visit(IkeaDiv div) {
		return "div [ %s | %s ] -> %s".formatted(reg(div.getRight().box()), reg(div.getLeft().box()), reg(div.box()));
	}

	@Override
	public String visit(IkeaMod mod) {
		return "mod [ %s | %s ] -> %s".formatted(reg(mod.getRight().box()), reg(mod.getLeft().box()), reg(mod.box()));
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

	private String reg(IkeaBøx box) {
		if (box instanceof IkeaVirtualRegister register) {
			return "%@" + register.num() + register.size().getNewRegisterSuffix();
		}
		if (box instanceof IkeaImmediate immediate) {
			return "$" + immediate.assemblyName();
		}
		throw new InternalCompilerException("Got: " + box);
	}
}
