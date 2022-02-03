package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.register.CalleeSavedPrepare;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import com.github.firmwehr.gentle.backend.lego.register.X86Register;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import firm.nodes.Node;

import java.util.ArrayList;
import java.util.List;

import static com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement.singleRegister;

public class LegoRet extends LegoNode {

	public LegoRet(
		int id, LegoPlate block, LegoGraph graph, List<Node> firmNodes
	) {
		super(id, block, graph, LegoBøx.LegoRegisterSize.ILLEGAL, firmNodes);
	}

	@Override
	public <T> T accept(LegoVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<LegoRegisterRequirement> inRequirements() {
		List<LegoRegisterRequirement> requirements = new ArrayList<>();
		List<LegoNode> inputs = inputs();

		if (inputs.size() > CalleeSavedPrepare.CALLEE_SAVED.size()) {
			// Return value is in RAX
			requirements.add(singleRegister(X86Register.RAX));
		}
		// We need to keep them in the same registers (order is relevant here!)
		for (X86Register register : CalleeSavedPrepare.CALLEE_SAVED) {
			requirements.add(singleRegister(register));
		}

		return requirements;
	}

	@Override
	public LegoRegisterRequirement registerRequirement() {
		return LegoRegisterRequirement.none();
	}

}
