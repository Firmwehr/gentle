package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Address;
import firm.nodes.Call;
import firm.nodes.Node;

import java.util.List;

public record IkeaCall(
	IkeaBøx box,
	Address address,
	List<IkeaNode> arguments,
	Call call
) implements IkeaNode {

	@Override
	public List<IkeaNode> parents() {
		return arguments();
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<Node> getUnderlyingFirmNodes() {
		return List.of(call);
	}

}
