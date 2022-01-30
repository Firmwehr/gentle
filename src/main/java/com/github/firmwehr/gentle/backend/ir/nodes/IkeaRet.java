package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.CalleeSavedPrepare;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.register.X86Register;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.ArrayList;
import java.util.List;

import static com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement.singleRegister;

public class IkeaRet extends IkeaNode {

	public IkeaRet(
		int id, IkeaBløck block, IkeaGraph graph, List<Node> firmNodes
	) {
		super(id, block, graph, IkeaBøx.IkeaRegisterSize.ILLEGAL, firmNodes);
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		List<IkeaRegisterRequirement> requirements = new ArrayList<>();
		List<IkeaNode> inputs = inputs();

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
	public IkeaRegisterRequirement registerRequirement() {
		return IkeaRegisterRequirement.none();
	}

}
