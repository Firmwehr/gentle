package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;

public interface IkeaNode {

	IkeaBøx box();

	List<IkeaNode> parents();

	<T> T accept(IkeaVisitor<T> visitor);

	List<Node> getUnderlyingFirmNodes();
}
