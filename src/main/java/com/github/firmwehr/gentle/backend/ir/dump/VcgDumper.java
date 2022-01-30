package com.github.firmwehr.gentle.backend.ir.dump;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.IkeaParentBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaAdd;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaArgNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaCall;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaCmp;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaConst;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaConv;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaCopy;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaDiv;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaJcc;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaJmp;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovLoad;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovLoadEx;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovRegister;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovStore;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovStoreEx;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMul;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPerm;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPhi;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaProj;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaReload;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaRet;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaShl;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaShr;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaShrs;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaSpill;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaSub;
import com.github.firmwehr.gentle.backend.ir.register.ControlFlowGraph;
import com.github.firmwehr.gentle.cli.CompilerArguments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class VcgDumper {

	private final ControlFlowGraph controlFlowGraph;
	private final IkeaGraph ikeaGraph;

	public VcgDumper(ControlFlowGraph controlFlowGraph, IkeaGraph ikeaGraph) {
		this.controlFlowGraph = controlFlowGraph;
		this.ikeaGraph = ikeaGraph;
	}

	public String dumpGraphAsString() {
		StringBuilder result = new StringBuilder();

		result.append("graph: {");
		String graphName = controlFlowGraph.getStart().origin().getGraph().getEntity().getLdName();
		result.append("\n  title: ").append('"').append(graphName).append('"').append("\n");

		result.append("""
			display_edge_labels: yes
			layoutalgorithm: mindepth //$ "Compilergraph"
			manhattan_edges: yes
			port_sharing: no
			orientation: bottom_to_top
			""".indent(2));

		for (VcgColor color : VcgColor.values()) {
			result.append("\n  colorentry ").append(color.id()).append(": ").append(color.getRgb());
		}

		result.append("\n");

		result.append(formatMethod(graphName).indent(2));

		result.append("}");

		return result.toString();
	}

	private String formatMethod(String name) {
		StringBuilder result = new StringBuilder();

		result.append("graph: {");
		result.append("\n  title: ").append('"').append("method").append('"');
		result.append("\n  label: ").append('"').append(name).append('"');
		result.append("\n  color: ").append(VcgColor.ROOT_BLOCK.id());

		for (IkeaBløck block : controlFlowGraph.getAllBlocks()) {
			result.append("\n").append(formatBlock(block).indent(2));
		}

		result.append("}");

		return result.toString();
	}

	private String formatBlock(IkeaBløck block) {
		StringBuilder result = new StringBuilder("graph: {");
		result.append("\n  title: " + '"').append(blockTitle(block)).append('"');
		result.append("\n  label: " + '"').append(block.origin()).append('"');
		result.append("\n  status: clustered");
		result.append("\n  color: ").append(VcgColor.BLOCK.id());
		result.append("\n");

		for (IkeaNode node : block.nodes()) {
			result.append(formatNode(node).indent(2));
			result.append(formatInputEdges(node).indent(2));
		}
		result.append(formatControlflowEdges(block));

		if (CompilerArguments.get().dumpBackendSchedule()) {
			result.append(formatSchedule(block));
		}

		result.append("\n}");

		return result.toString();
	}

	private String formatNode(IkeaNode node) {
		String infoText = "Firm nodes:\n" +
			node.underlyingFirmNodes().stream().map(Objects::toString).collect(Collectors.joining("\n")).indent(2);

		if (!node.registerIgnore()) {
			infoText += "Reg: " + node.register().get().map(Enum::name).orElse("<none>");
			if (node.registerRequirement().limited()) {
				infoText += "\n  limited to: " +
					node.registerRequirement().limitedTo().stream().map(Enum::name).collect(Collectors.joining(", "));
			}
		}

		String result = "node: {";
		result += "\n  title: " + '"' + nodeTitle(node) + '"' + "\n";
		result += "\n  label: " + '"' + nodeLabel(node) + '"' + "\n";
		result += "\n  color: " + nodeColor(node).id();
		result += "\n  info1: " + '"' + infoText + '"';
		result += "\n}";

		return result;
	}

	private String formatInputEdges(IkeaNode node) {
		return formatEdges(ikeaGraph.getInputEdges(node), "\n  priority: 50");
	}

	private String formatControlflowEdges(IkeaBløck block) {
		StringJoiner result = new StringJoiner("\n");
		List<IkeaParentBløck> parents = block.parents();
		for (int i = 0; i < parents.size(); i++) {
			IkeaParentBløck parent = parents.get(i);

			for (IkeaNode node : parent.parent().nodes()) {
				if (node instanceof IkeaJcc jcc) {
					if (jcc.trueTarget().equals(block)) {
						result.add(formatControlflowEdge(block, jcc, "True: " + i));
					} else {
						result.add(formatControlflowEdge(block, jcc, "False: " + i));
					}
				} else if (node instanceof IkeaJmp jmp) {
					result.add(formatControlflowEdge(jmp.target(), jmp, String.valueOf(i)));
				}

			}
		}

		return result.toString();
	}

	private String formatControlflowEdge(IkeaBløck source, IkeaNode dst, String label) {
		String result = "edge: {";
		result += "\n  sourcename: " + '"' + blockTitle(source) + '"';
		result += "\n  targetname: " + '"' + nodeTitle(dst) + '"';
		result += "\n  label: " + '"' + label + '"';
		result += "\n  color: " + VcgColor.CONTROL_FLOW.id();
		result += "\n}";
		return result;
	}

	private String formatEdges(Collection<IkeaGraph.IkeaEdge> edges, String additionalProps) {
		StringJoiner result = new StringJoiner("\n");
		for (IkeaGraph.IkeaEdge edge : edges) {
			StringBuilder inner = new StringBuilder();
			// edge: {sourcename: "n74" targetname: "n71" label: "0" class:14 priority:50 color:blue}
			inner.append("edge: {");
			inner.append("\n  sourcename: ").append('"').append(nodeTitle(edge.src())).append('"');
			inner.append("\n  targetname: ").append('"').append(nodeTitle(edge.dst())).append('"');
			inner.append("\n  label: ").append('"').append(edge.index()).append('"');
			inner.append(additionalProps);
			inner.append("\n}");
			result.add(inner);
		}

		return result.toString();
	}

	private String formatSchedule(IkeaBløck block) {
		List<IkeaGraph.IkeaEdge> edges = new ArrayList<>();
		List<IkeaNode> nodes = block.nodes();
		for (int i = 0; i < nodes.size() - 1; i++) {
			IkeaNode src = nodes.get(i);
			IkeaNode dst = nodes.get(i + 1);

			edges.add(new IkeaGraph.IkeaEdge(dst, src, 0));
		}

		return formatEdges(edges, "\n  color: " + VcgColor.SCHEDULE.id());
	}

	@SuppressWarnings("DuplicateBranchesInSwitch")
	private VcgColor nodeColor(IkeaNode node) {
		return switch (node) {
			case IkeaAdd ignored -> VcgColor.NORMAL;
			case IkeaArgNode ignored -> VcgColor.SPECIAL;
			case IkeaCall ignored -> VcgColor.MEMORY;
			case IkeaCmp ignored -> VcgColor.NORMAL;
			case IkeaConst ignored -> VcgColor.CONST;
			case IkeaConv ignored -> VcgColor.NORMAL;
			case IkeaCopy ignored -> VcgColor.SPECIAL;
			case IkeaDiv ignored -> VcgColor.MEMORY;
			case IkeaJcc ignored -> VcgColor.CONTROL_FLOW;
			case IkeaJmp ignored -> VcgColor.CONTROL_FLOW;
			case IkeaMovLoad ignored -> VcgColor.MEMORY;
			case IkeaMovLoadEx ignored -> VcgColor.MEMORY;
			case IkeaMovRegister ignored -> VcgColor.SPECIAL;
			case IkeaMovStore ignored -> VcgColor.MEMORY;
			case IkeaMovStoreEx ignored -> VcgColor.MEMORY;
			case IkeaMul ignored -> VcgColor.NORMAL;
			case IkeaPerm ignored -> VcgColor.SPECIAL;
			case IkeaPhi ignored -> VcgColor.PHI;
			case IkeaProj ignored -> VcgColor.SPECIAL;
			case IkeaReload ignored -> VcgColor.MEMORY;
			case IkeaRet ignored -> VcgColor.CONTROL_FLOW;
			case IkeaShl ignored -> VcgColor.NORMAL;
			case IkeaShr ignored -> VcgColor.NORMAL;
			case IkeaShrs ignored -> VcgColor.NORMAL;
			case IkeaSpill ignored -> VcgColor.MEMORY;
			case IkeaSub ignored -> VcgColor.NORMAL;
			default -> throw new InternalCompilerException("Unexpected value: " + node);
		};
	}

	private String nodeTitle(IkeaNode node) {
		return "node-" + node.id();
	}

	private String nodeLabel(IkeaNode node) {
		return node.toString();
	}

	private String blockTitle(IkeaBløck block) {
		return "block-" + block.origin().getNr();
	}

	private enum VcgColor {
		// colorentry 100: 204 204 204  gray
		// colorentry 101: 222 239 234  faint green
		// colorentry 103: 242 242 242  white-ish
		// colorentry 104: 153 255 153  light green
		// colorentry 105: 153 153 255  blue
		// colorentry 106: 255 153 153  red
		// colorentry 107: 255 255 153  yellow
		// colorentry 108: 255 153 255  pink
		// colorentry 110: 127 127 127  dark gray
		// colorentry 111: 153 255 153  light green
		// colorentry 114: 153 153 255  blue
		CONTROL_FLOW("255 153 153"),
		MEMORY("153 153 255"),
		NORMAL("242 242 242"),
		SPECIAL("255 153 255"),
		CONST("255 255 153"),
		PHI("153 255 153"),
		ROOT_BLOCK("204 204 204"),
		BLOCK("222 239 234"),
		SCHEDULE("255 153 255");

		private final String rgb;

		VcgColor(String rgb) {
			this.rgb = rgb;
		}

		public String getRgb() {
			return rgb;
		}

		public int id() {
			return 100 + ordinal();
		}
	}
}
