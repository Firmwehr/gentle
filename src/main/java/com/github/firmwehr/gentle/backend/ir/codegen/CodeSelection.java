package com.github.firmwehr.gentle.backend.ir.codegen;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize;
import com.github.firmwehr.gentle.backend.ir.IkeaImmediate;
import com.github.firmwehr.gentle.backend.ir.IkeaParentBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaUnassignedBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaVirtualRegister;
import com.github.firmwehr.gentle.backend.ir.nodes.BoxScheme;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaAdd;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaAnd;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaArgNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaCall;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaCmp;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaConst;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaConv;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaDiv;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaJcc;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaJmp;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovLoad;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovLoadEx;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovRegister;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovStore;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovStoreEx;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMul;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNeg;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPhi;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaRet;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaSet;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaShl;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaShr;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaShrs;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaSub;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaXor;
import com.github.firmwehr.gentle.firm.Util;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.Pair;
import firm.BackEdges;
import firm.Graph;
import firm.MethodType;
import firm.Mode;
import firm.Relation;
import firm.nodes.Add;
import firm.nodes.Address;
import firm.nodes.And;
import firm.nodes.Block;
import firm.nodes.Call;
import firm.nodes.Cmp;
import firm.nodes.Cond;
import firm.nodes.Const;
import firm.nodes.Conv;
import firm.nodes.Div;
import firm.nodes.End;
import firm.nodes.Eor;
import firm.nodes.Jmp;
import firm.nodes.Load;
import firm.nodes.Minus;
import firm.nodes.Mod;
import firm.nodes.Mul;
import firm.nodes.Mux;
import firm.nodes.NoMem;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Phi;
import firm.nodes.Proj;
import firm.nodes.Return;
import firm.nodes.Shl;
import firm.nodes.Shr;
import firm.nodes.Shrs;
import firm.nodes.Start;
import firm.nodes.Store;
import firm.nodes.Sub;
import firm.nodes.Unknown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CodeSelection extends NodeVisitor.Default {

	private static final Logger LOGGER = new Logger(CodeSelection.class, Logger.LogLevel.DEBUG);

	private final Map<Block, IkeaBløck> blocks;
	private final Map<Block, List<Phi>> phiBär;
	private final Map<Node, IkeaNode> nodes;

	private final Graph graph;
	private final CodePreselection preselection;
	private int regCount;

	public CodeSelection(Graph graph, CodePreselection preselection) {
		this.graph = graph;
		this.preselection = preselection;
		this.blocks = new HashMap<>();
		this.phiBär = new HashMap<>();
		this.nodes = new HashMap<>();
		this.regCount = ((MethodType) this.graph.getEntity().getType()).getNParams();
	}

	public List<IkeaBløck> convertBlocks() {
		LOGGER.info("Converting blocks for %s", graph.getEntity().getLdName());
		CriticalEdges.breakCriticalEdges(graph);

		BackEdges.enable(graph);
		graph.walkBlocks(block -> blocks.put(block, new IkeaBløck(new ArrayList<>(), new ArrayList<>(), block)));
		graph.walkTopological(new Default() {
			@Override
			public void visit(Phi node) {
				if (node.getMode().equals(Mode.getM())) {
					return;
				}
				IkeaPhi ikeaPhi = new IkeaPhi(nextRegister(node), node);
				nodes.put(node, ikeaPhi);
				phiBär.computeIfAbsent((Block) node.getBlock(), ignore -> new ArrayList<>()).add(node);
			}
		});
		graph.walkTopological(this);
		graph.walkBlocks(block -> {
			for (int i = 0, c = block.getPredCount(); i < c; i++) {
				Block pred = (Block) block.getPred(i).getBlock();
				LOGGER.debugHeader("Moves for %s <- %s", block.getNr(), pred.getNr());
				RegisterTransferGraph transferGraph = new RegisterTransferGraph(Set.of(nextRegister(Mode.getLs())));
				IkeaBløck ikeaPred = blocks.get(pred);

				for (Phi phi : phiBär.getOrDefault(block, List.of())) {
					IkeaNode ikeaNode = nodes.get(phi.getPred(i));
					IkeaBøx target = nodes.get(phi).box();
					IkeaBøx source = ikeaNode.box();
					transferGraph.addMove(source, target);
				}

				for (Pair<IkeaBøx, IkeaBøx> pair : transferGraph.generateMoveSequence()) {
					ikeaPred.nodes().add(new IkeaMovRegister(pair.first(), pair.second()));
				}

				blocks.get(block).parents().add(new IkeaParentBløck(ikeaPred));
			}
		});
		BackEdges.disable(graph);

		// Move jmp / return to end of block
		for (IkeaBløck block : blocks.values()) {
			List<IkeaNode> jumps = block.nodes()
				.stream()
				.filter(it -> it instanceof IkeaJcc || it instanceof IkeaJmp || it instanceof IkeaRet ||
					(it instanceof IkeaCmp cmp && cmp.belongsToJump()))
				.toList();
			block.nodes().removeAll(jumps);
			block.nodes().addAll(jumps);
		}

		List<IkeaBløck> orderedBlocks = blocks.values()
			.stream()
			.filter(it -> !it.origin().equals(graph.getEndBlock()))
			.collect(Collectors.toCollection(ArrayList::new));
		orderedBlocks.remove(blocks.get(graph.getStartBlock()));
		orderedBlocks.add(0, blocks.get(graph.getStartBlock()));
		return orderedBlocks;
	}

	private IkeaBøx nextRegister(Node node) {
		return nextRegister(node.getMode());
	}

	private IkeaBøx nextRegister(Mode mode) {
		return new IkeaVirtualRegister(this.regCount++, IkeaRegisterSize.forMode(mode));
	}

	@Override
	public void visit(Add node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaAdd ikeaAdd = new IkeaAdd(nextRegister(node), nodes.get(node.getLeft()), nodes.get(node.getRight()), node);
		nodes.put(node, ikeaAdd);
		block.nodes().add(ikeaAdd);
	}

	@Override
	public void visit(And node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaAnd ikeaAdd = new IkeaAnd(nextRegister(node), nodes.get(node.getLeft()), nodes.get(node.getRight()), node);
		nodes.put(node, ikeaAdd);
		block.nodes().add(ikeaAdd);
	}

	@Override
	public void visit(Block node) {
		// Ignored here. Only added for completeness, as this visitor verifies no node is missed
	}

	@Override
	public void visit(Start node) {
		// Ignored here. Only added for completeness, as this visitor verifies no node is missed
	}

	@Override
	public void visit(End node) {
		// Ignored here. Only added for completeness, as this visitor verifies no node is missed
	}

	@Override
	public void visit(Address node) {
		// Ignored here. Only added for completeness, as this visitor verifies no node is missed
		// Address is generated by calls
	}

	@Override
	public void visit(Phi node) {
		// Ignored here. Only added for completeness, as this visitor verifies no node is missed
		// Phis are handled in a separate phase
	}

	@Override
	public void visit(NoMem node) {
		// Ignored here. Only added for completeness, as this visitor verifies no node is missed
	}

	@Override
	public void visit(Call node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		List<IkeaNode> arguments = Util.predsStream(node)
			.filter(it -> !it.getMode().equals(Mode.getM()))
			.filter(it -> !(it instanceof Address))
			.map(nodes::get)
			.toList();
		MethodType type = (MethodType) node.getType();
		IkeaBøx box = new IkeaUnassignedBøx(IkeaRegisterSize.ILLEGAL);
		if (type.getNRess() == 1 && !type.getResType(0).getMode().equals(Mode.getANY())) {
			box = nextRegister(type.getResType(0).getMode());
		}
		IkeaCall ikeaCall = new IkeaCall(box, (Address) node.getPred(1/*not a magic value!*/), arguments, node);
		nodes.put(node, ikeaCall);
		block.nodes().add(ikeaCall);
	}

	@Override
	public void visit(Cmp node) {
		// x86 uses special registers for conditional jump, so cmp result needs to be preceeding conditional jump
	}

	private IkeaCmp visitFromCond(Cmp node, boolean fromJumo) {
		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaNode left = nodes.get(node.getLeft());
		IkeaNode right = nodes.get(node.getRight());
		boolean wasInverted = false;
		// Immediate has to be on the right
		if (node.getLeft() instanceof Const) {
			IkeaNode tmp = left;
			left = right;
			right = tmp;
			wasInverted = true;
		}

		return new IkeaCmp(left, right, node, wasInverted, fromJumo);
	}

	@Override
	public void visit(Cond node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		List<Node> nodes = Util.outsStream(node).toList();
		Node trueProj = ((Proj) nodes.get(0)).getNum() == Cond.pnTrue ? nodes.get(0) : nodes.get(1);
		Node falseProj = ((Proj) nodes.get(0)).getNum() == Cond.pnFalse ? nodes.get(0) : nodes.get(1);

		BackEdges.Edge trueEdge = BackEdges.getOuts(trueProj).iterator().next();
		Block trueBlock = (Block) trueEdge.node;
		BackEdges.Edge falseEdge = BackEdges.getOuts(falseProj).iterator().next();
		Block falseBlock = (Block) falseEdge.node;

		Relation relation = ((Cmp) node.getSelector()).getRelation();

		IkeaCmp cmp = visitFromCond((Cmp) node.getSelector(), true);

		if (cmp.wasInverted()) {
			relation = relation.inversed();
		}

		IkeaJcc ikeaJcc = new IkeaJcc(blocks.get(trueBlock), blocks.get(falseBlock), node, relation, cmp);
		this.nodes.put(node, ikeaJcc);
		block.nodes().add(cmp);
		block.nodes().add(ikeaJcc);
	}

	@Override
	public void visit(Const node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaImmediate box = new IkeaImmediate(node.getTarval(), IkeaRegisterSize.forMode(node.getMode()));
		IkeaConst ikeaConst = new IkeaConst(box, node);
		nodes.put(node, ikeaConst);
		block.nodes().add(ikeaConst);
	}

	@Override
	public void visit(Conv node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaRegisterSize sourceSize = IkeaRegisterSize.forMode(node.getOp().getMode());
		IkeaRegisterSize targetSize = IkeaRegisterSize.forMode(node.getMode());
		IkeaConv ikeaConv = new IkeaConv(nextRegister(node), nodes.get(node.getOp()), sourceSize, targetSize, node);
		nodes.put(node, ikeaConv);
		block.nodes().add(ikeaConv);
	}

	@Override
	public void visit(Div node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		// @formatter:off
		IkeaDiv ikeaDiv = new IkeaDiv(
			nextRegister(node.getResmode()),
			nextRegister(node.getResmode()),
			nodes.get(node.getLeft()),
			nodes.get(node.getRight()),
			node,
			IkeaDiv.Result.QUOTIENT
		);
		// @formatter:on
		nodes.put(node, ikeaDiv);
		block.nodes().add(ikeaDiv);
	}

	@Override
	public void visit(Eor node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaXor ikeaXor =
			new IkeaXor(nextRegister(node.getMode()), nodes.get(node.getLeft()), nodes.get(node.getRight()), node);
		nodes.put(node, ikeaXor);
		block.nodes().add(ikeaXor);
	}

	@Override
	public void visit(Jmp node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		for (BackEdges.Edge edge : BackEdges.getOuts(node)) {
			if (edge.node instanceof Block targetBlock) {
				IkeaJmp ikeaJmp = new IkeaJmp(blocks.get(targetBlock), node);
				nodes.put(node, ikeaJmp);
				block.nodes().add(ikeaJmp);
			}
		}
	}

	@Override
	public void visit(Load node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());

		var maybeScheme = preselection.scheme(node);
		if (maybeScheme.isPresent()) {
			var scheme = maybeScheme.get();

			var mode = node.getLoadMode();
			IkeaMovLoadEx mov =
				new IkeaMovLoadEx(nextRegister(mode), node, BoxScheme.fromAddressingScheme(scheme, nodes::get));
			nodes.put(node, mov);
			block.nodes().add(mov);

			return;
		}

		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		Mode mode = node.getLoadMode();
		IkeaMovLoad ikeaMovLoad =
			new IkeaMovLoad(nextRegister(mode), nodes.get(node.getPtr()), IkeaRegisterSize.forMode(mode), node);
		nodes.put(node, ikeaMovLoad);
		block.nodes().add(ikeaMovLoad);
	}

	@Override
	public void visit(Minus node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaNeg ikeaNeg = new IkeaNeg(nextRegister(node), nodes.get(node.getOp()), node);
		nodes.put(node, ikeaNeg);
		block.nodes().add(ikeaNeg);
	}

	@Override
	public void visit(Mod node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		// @formatter:off
		IkeaDiv ikeaDiv = new IkeaDiv(
			nextRegister(node.getResmode()),
			nextRegister(node.getResmode()),
			nodes.get(node.getLeft()),
			nodes.get(node.getRight()),
			node,
			IkeaDiv.Result.MOD
		);
		// @formatter:on
		nodes.put(node, ikeaDiv);
		block.nodes().add(ikeaDiv);
	}

	@Override
	public void visit(Mul node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaMul ikeaMul = new IkeaMul(nextRegister(node), nodes.get(node.getLeft()), nodes.get(node.getRight()), node);
		nodes.put(node, ikeaMul);
		block.nodes().add(ikeaMul);
	}

	@Override
	public void visit(Mux node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaCmp ikeaCmp = visitFromCond((Cmp) node.getSel(), false);
		IkeaSet ikeaSet = new IkeaSet(nextRegister(node), node, ikeaCmp, ((Cmp) node.getSel()).getRelation());
		nodes.put(node, ikeaSet);
		block.nodes().add(ikeaCmp);
		block.nodes().add(ikeaSet);
	}

	@Override
	public void visit(Proj node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		if (node.getMode().equals(Mode.getM()) || node.getMode().equals(Mode.getX()) ||
			node.getPred() instanceof Start || node.getPred() instanceof Call) {
			return;
		}
		if (node.getPred() instanceof Div div) {
			nodes.put(node, nodes.get(div));
		} else if (node.getPred() instanceof Mod mod) {
			nodes.put(node, nodes.get(mod));
		} else if (node.getPred() instanceof Load load) {
			nodes.put(node, nodes.get(load));
		} else if (node.getPred() instanceof Proj proj && proj.getPred() instanceof Start) {
			visitArgument(node);
		} else if (node.getPred() instanceof Proj proj && proj.getPred() instanceof Call call) {
			nodes.put(node, nodes.get(call));
		} else {
			throw new InternalCompilerException("Unexpected Proj. It's time to run. " + node);
		}
	}

	private void visitArgument(Proj proj) {
		// no need to check for replaced node since caller will already check for us

		IkeaBløck block = blocks.get((Block) proj.getBlock());
		IkeaVirtualRegister box = new IkeaVirtualRegister(proj.getNum(), IkeaRegisterSize.forMode(proj.getMode()));
		IkeaArgNode ikeaArgNode = new IkeaArgNode(box, proj);
		nodes.put(proj, ikeaArgNode);
		block.nodes().add(ikeaArgNode);
	}

	@Override
	public void visit(Return node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaRet ikeaRet = new IkeaRet(
			Util.predsStream(node).filter(n -> !n.getMode().equals(Mode.getM())).findFirst().map(nodes::get), node);
		nodes.put(node, ikeaRet);
		block.nodes().add(ikeaRet);
	}

	@Override
	public void visit(Store node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());

		var value = nodes.get(node.getValue());

		var maybeScheme = preselection.scheme(node);
		if (maybeScheme.isPresent()) {
			var scheme = maybeScheme.get();

			IkeaMovStoreEx mov = new IkeaMovStoreEx(value, node, BoxScheme.fromAddressingScheme(scheme, nodes::get));
			nodes.put(node, mov);
			block.nodes().add(mov);

			return;
		}

		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}


		IkeaRegisterSize size = IkeaRegisterSize.forMode(node.getValue().getMode());
		IkeaMovStore ikeaMovStore = new IkeaMovStore(value, nodes.get(node.getPtr()), size, node);
		nodes.put(node, ikeaMovStore);
		block.nodes().add(ikeaMovStore);
	}

	@Override
	public void visit(Shl node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaShl ikeaShl = new IkeaShl(nextRegister(node), nodes.get(node.getLeft()), nodes.get(node.getRight()), node);
		nodes.put(node, ikeaShl);
		block.nodes().add(ikeaShl);
	}

	@Override
	public void visit(Shr node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaShr ikeaShr = new IkeaShr(nextRegister(node), nodes.get(node.getLeft()), nodes.get(node.getRight()), node);
		nodes.put(node, ikeaShr);
		block.nodes().add(ikeaShr);
	}

	@Override
	public void visit(Shrs node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaShrs ikeaShrs =
			new IkeaShrs(nextRegister(node), nodes.get(node.getLeft()), nodes.get(node.getRight()), node);
		nodes.put(node, ikeaShrs);
		block.nodes().add(ikeaShrs);
	}

	@Override
	public void visit(Sub node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaSub ikeaSub = new IkeaSub(nextRegister(node), nodes.get(node.getLeft()), nodes.get(node.getRight()), node);
		nodes.put(node, ikeaSub);
		block.nodes().add(ikeaSub);
	}

	@Override
	public void visit(Unknown node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaRegisterSize size = IkeaRegisterSize.forMode(node.getMode());
		IkeaImmediate immediate = new IkeaImmediate(node.getMode().getNull(), size);
		IkeaConst ikeaConst = new IkeaConst(immediate, node);
		nodes.put(node, ikeaConst);
		block.nodes().add(ikeaConst);
	}

	@Override
	public void defaultVisit(Node n) {
		throw new InternalCompilerException("Unexpected node " + n);
	}
}
