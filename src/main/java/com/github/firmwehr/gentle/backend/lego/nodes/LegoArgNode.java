package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import com.google.common.base.Preconditions;
import firm.nodes.Node;

import java.util.List;

public class LegoArgNode extends LegoNode {

	private static final boolean USE_CDECL = true;

	private final int index;

	public LegoArgNode(
		int id, LegoPlate block, LegoGraph graph, LegoBøx.LegoRegisterSize size, List<Node> firmNodes, int index
	) {
		super(id, block, graph, size, firmNodes);
		this.index = index;
	}

	@Override
	public <T> T accept(LegoVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<LegoRegisterRequirement> inRequirements() {
		return List.of();
	}

	/**
	 * Returns the offset of this argument in the stack.
	 */
	public int stackOffset() {
		int offset = LegoCall.REGISTER_ORDER.size();
		if (USE_CDECL) {
			offset = 0;
		}
		Preconditions.checkState(!isPassedInRegister(), "Not on stack");
		return (index - offset) * 8 + 16;
	}

	@Override
	public LegoRegisterRequirement registerRequirement() {
		// passed in a register
		if (isPassedInRegister()) {
			return LegoRegisterRequirement.singleRegister(LegoCall.REGISTER_ORDER.get(index));
		}
		// passed on the stack, just reserve whatever register for it
		return LegoRegisterRequirement.gpRegister();
	}

	public boolean isPassedInRegister() {
		if (USE_CDECL) {
			return false;
		}
		return index < LegoCall.REGISTER_ORDER.size();
	}

	@Override
	public String display() {
		return "LegoArg " + index + " (" + id() + ")";
	}
}
