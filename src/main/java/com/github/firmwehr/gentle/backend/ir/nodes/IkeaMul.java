package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;

public class IkeaMul extends IkeaBinaryOp {

	/**
	 * @param id The id of the node.
	 * @param block The parent block of the node.
	 * @param graph The associated graph.
	 * @param size The register size of the resulting value.
	 * @param firmNodes A list of firm nodes that are part of this ikea node.
	 */
	public IkeaMul(
		int id, IkeaBløck block, IkeaGraph graph, IkeaBøx.IkeaRegisterSize size, List<Node> firmNodes
	) {
		super(id, block, graph, size, firmNodes);
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

}
