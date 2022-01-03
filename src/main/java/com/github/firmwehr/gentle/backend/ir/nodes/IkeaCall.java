package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import firm.nodes.Address;
import firm.nodes.Call;

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
}
