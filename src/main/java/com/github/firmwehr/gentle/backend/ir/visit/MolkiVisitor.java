package com.github.firmwehr.gentle.backend.ir.visit;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaImmediate;
import com.github.firmwehr.gentle.backend.ir.IkeaVirtualRegister;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaAdd;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaCall;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaCmp;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaConst;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaJcc;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaJmp;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaRet;
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
	public String visit(IkeaRet ret) {
		String result = "";
		if (ret.getValue().isPresent()) {
			result += "mov %s, %%@r0".formatted(reg(ret.getValue().get().box()));
		}
		result += "\nreturn";
		return result;
	}

	@Override
	public String visit(IkeaCall call) {
		String result = "call %s".formatted(call.address().getEntity().getLdName()) + " ";
		result += call.arguments().stream().map(it -> reg(it.box())).collect(joining(" | ", "[ ", " " + "]"));

		if (!isVoid(call.address().getEntity())) {
			result += " -> " + reg(call.box());
		}

		return result;
	}

	@Override
	public String visit(IkeaCmp cmp) {
		return "cmp %s, %s".formatted(reg(cmp.getLeft().box()), reg(cmp.getRight().box()));
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
	public String visit(IkeaBløck block) {
		StringJoiner result = new StringJoiner("\n");

		for (IkeaNode node : block.nodes()) {
			if (node instanceof IkeaConst) {
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
		result += "\n.endfunction";

		return result;
	}

	private String blockMarker(IkeaBløck block) {
		return ".block" + block.id();
	}

	private boolean isVoid(Entity entity) {
		return ((MethodType) entity.getType()).getResType(0).getMode().equals(Mode.getANY());
	}

	private String reg(IkeaBøx box) {
		if (box instanceof IkeaVirtualRegister register) {
			return "%@" + register.num();
		}
		if (box instanceof IkeaImmediate immediate) {
			return "$" + immediate.assemblyName();
		}
		throw new InternalCompilerException("Got: " + box);
	}
}
