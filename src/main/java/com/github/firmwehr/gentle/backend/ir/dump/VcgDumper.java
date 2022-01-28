package com.github.firmwehr.gentle.backend.ir.dump;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
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
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaLea;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class VcgDumper {

	private final ControlFlowGraph controlFlowGraph;
	private final IkeaGraph ikeaGraph;
	private final Map<IkeaNode, Integer> nodeIds;

	public VcgDumper(ControlFlowGraph controlFlowGraph, IkeaGraph ikeaGraph) {
		this.controlFlowGraph = controlFlowGraph;
		this.ikeaGraph = ikeaGraph;

		this.nodeIds = new HashMap<>();
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
		result.append("\n  color: ").append(VcgColor.BLOCK.id());

		for (IkeaBløck block : controlFlowGraph.getAllBlocks()) {
			result.append("\n").append(formatBlock(block).indent(2));
		}

		result.append("}");

		return result.toString();
	}

	private String formatBlock(IkeaBløck block) {
		StringBuilder result = new StringBuilder("graph: {");
		result.append("\n  title: " + '"' + "block-" + block.origin().getNr() + '"');
		result.append("\n  label: " + '"' + block.origin() + '"');
		result.append("\n  status: clustered");
		result.append("\n  color: ").append(VcgColor.BLOCK.id());
		result.append("\n");

		for (IkeaNode node : block.nodes()) {
			result.append(formatNode(node).indent(2));
			result.append(formatInputEdges(node).indent(2));
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
		StringJoiner result = new StringJoiner("\n");
		for (IkeaGraph.IkeaEdge edge : ikeaGraph.getInputEdges(node)) {
			StringBuilder inner = new StringBuilder();
			// edge: {sourcename: "n74" targetname: "n71" label: "0" class:14 priority:50 color:blue}
			inner.append("edge: {");
			inner.append("\n sourcename: ").append('"').append(nodeTitle(edge.src())).append('"');
			inner.append("\n targetname: ").append('"').append(nodeTitle(edge.dst())).append('"');
			inner.append("\n}");
			result.add(inner);
		}

		return result.toString();
	}

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
			case IkeaLea ignored -> VcgColor.NORMAL;
			case IkeaMovLoad ignored -> VcgColor.MEMORY;
			case IkeaMovLoadEx ignored -> VcgColor.MEMORY;
			case IkeaMovRegister ignored -> VcgColor.NORMAL;
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
		int id = nodeIds.computeIfAbsent(node, ignored -> nodeIds.size());

		return "node-" + id;
	}

	private String nodeLabel(IkeaNode node) {
		return node.toString();
	}

	private enum VcgColor {
		// colorentry 100: 204 204 204 gray
		//colorentry 101: 222 239 234  faint green
		//colorentry 103: 242 242 242  white-ish
		//colorentry 104: 153 255 153  light green
		//colorentry 105: 153 153 255  blue
		//colorentry 106: 255 153 153  red
		//colorentry 107: 255 255 153  yellow
		//colorentry 108: 255 153 255  pink
		//colorentry 110: 127 127 127  dark gray
		//colorentry 111: 153 255 153  light green
		//colorentry 114: 153 153 255  blue
		CONTROL_FLOW("255 153 153"),
		MEMORY("153 153 255"),
		NORMAL("242 242 242"),
		SPECIAL("222 239 234"),
		CONST("255 255 153"),
		PHI("153 255 153"),
		BLOCK("204 204 204");

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
