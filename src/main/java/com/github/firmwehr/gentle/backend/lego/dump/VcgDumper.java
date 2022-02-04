package com.github.firmwehr.gentle.backend.lego.dump;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.LegoParentBløck;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoAdd;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoAnd;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoArgNode;
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
import com.github.firmwehr.gentle.backend.lego.nodes.LegoShr;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoSpill;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoSub;
import com.github.firmwehr.gentle.backend.lego.register.ControlFlowGraph;
import com.github.firmwehr.gentle.cli.CompilerArguments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class VcgDumper {

	private final ControlFlowGraph controlFlowGraph;
	private final LegoGraph legoGraph;

	public VcgDumper(ControlFlowGraph controlFlowGraph, LegoGraph legoGraph) {
		this.controlFlowGraph = controlFlowGraph;
		this.legoGraph = legoGraph;
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

		for (LegoPlate block : controlFlowGraph.getAllBlocks()) {
			result.append("\n").append(formatBlock(block).indent(2));
		}

		result.append("}");

		return result.toString();
	}

	private String formatBlock(LegoPlate block) {
		StringBuilder result = new StringBuilder("graph: {");
		result.append("\n  title: " + '"').append(blockTitle(block)).append('"');
		result.append("\n  label: " + '"').append("Block ").append(block.origin().getNr()).append('"');
		result.append("\n  status: clustered");
		result.append("\n  color: ").append(VcgColor.BLOCK.id());
		result.append("\n");

		for (LegoNode node : block.nodes()) {
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

	private String formatNode(LegoNode node) {
		String infoText = "Firm nodes:\n" +
			node.underlyingFirmNodes().stream().map(Objects::toString).collect(Collectors.joining("\n")).indent(2);

		if (!node.registerIgnore()) {
			infoText += "Reg: " + node.register().map(Enum::name).orElse("<none>");
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

	private String formatInputEdges(LegoNode node) {
		return formatEdges(legoGraph.getInputEdges(node), "\n  priority: 50");
	}

	private String formatControlflowEdges(LegoPlate block) {
		StringJoiner result = new StringJoiner("\n");
		List<LegoParentBløck> parents = block.parents();
		for (int i = 0; i < parents.size(); i++) {
			LegoParentBløck parent = parents.get(i);

			for (LegoNode node : parent.parent().nodes()) {
				if (node instanceof LegoJcc jcc) {
					if (jcc.trueTarget().equals(block)) {
						result.add(formatControlflowEdge(block, jcc, "True: " + i));
					} else {
						result.add(formatControlflowEdge(block, jcc, "False: " + i));
					}
				} else if (node instanceof LegoJmp jmp) {
					result.add(formatControlflowEdge(jmp.target(), jmp, String.valueOf(i)));
				}

			}
		}

		return result.toString();
	}

	private String formatControlflowEdge(LegoPlate source, LegoNode dst, String label) {
		String result = "edge: {";
		result += "\n  sourcename: " + '"' + blockTitle(source) + '"';
		result += "\n  targetname: " + '"' + nodeTitle(dst) + '"';
		result += "\n  label: " + '"' + label + '"';
		result += "\n  color: " + VcgColor.CONTROL_FLOW.id();
		result += "\n}";
		return result;
	}

	private String formatEdges(Collection<LegoGraph.LegoEdge> edges, String additionalProps) {
		StringJoiner result = new StringJoiner("\n");
		for (LegoGraph.LegoEdge edge : edges) {
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

	private String formatSchedule(LegoPlate block) {
		List<LegoGraph.LegoEdge> edges = new ArrayList<>();
		List<LegoNode> nodes = block.nodes();
		for (int i = 0; i < nodes.size() - 1; i++) {
			LegoNode src = nodes.get(i);
			LegoNode dst = nodes.get(i + 1);

			edges.add(new LegoGraph.LegoEdge(dst, src, 0));
		}

		return formatEdges(edges, "\n  color: " + VcgColor.SCHEDULE.id());
	}

	@SuppressWarnings("DuplicateBranchesInSwitch")
	private VcgColor nodeColor(LegoNode node) {
		return switch (node) {
			case LegoAdd ignored -> VcgColor.NORMAL;
			case LegoAnd ignored -> VcgColor.NORMAL;
			case LegoArgNode ignored -> VcgColor.SPECIAL;
			case LegoCall ignored -> VcgColor.MEMORY;
			case LegoCmp ignored -> VcgColor.NORMAL;
			case LegoConst ignored -> VcgColor.CONST;
			case LegoConv ignored -> VcgColor.NORMAL;
			case LegoCopy ignored -> VcgColor.SPECIAL;
			case LegoDiv ignored -> VcgColor.MEMORY;
			case LegoImmediate ignored -> VcgColor.CONST;
			case LegoJcc ignored -> VcgColor.CONTROL_FLOW;
			case LegoJmp ignored -> VcgColor.CONTROL_FLOW;
			case LegoMovLoad ignored -> VcgColor.MEMORY;
			case LegoMovLoadEx ignored -> VcgColor.MEMORY;
			case LegoMovRegister ignored -> VcgColor.SPECIAL;
			case LegoMovStore ignored -> VcgColor.MEMORY;
			case LegoMovStoreEx ignored -> VcgColor.MEMORY;
			case LegoMul ignored -> VcgColor.NORMAL;
			case LegoNeg ignored -> VcgColor.NORMAL;
			case LegoPerm ignored -> VcgColor.SPECIAL;
			case LegoPhi ignored -> VcgColor.PHI;
			case LegoProj ignored -> VcgColor.SPECIAL;
			case LegoReload ignored -> VcgColor.MEMORY;
			case LegoRet ignored -> VcgColor.CONTROL_FLOW;
			case LegoSal ignored -> VcgColor.NORMAL;
			case LegoShr ignored -> VcgColor.NORMAL;
			case LegoSar ignored -> VcgColor.NORMAL;
			case LegoSpill ignored -> VcgColor.MEMORY;
			case LegoSub ignored -> VcgColor.NORMAL;
			default -> throw new InternalCompilerException("Unexpected value: " + node);
		};
	}

	private String nodeTitle(LegoNode node) {
		return "node-" + node.id();
	}

	private String nodeLabel(LegoNode node) {
		return node.toString();
	}

	private String blockTitle(LegoPlate block) {
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
