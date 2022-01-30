package com.github.firmwehr.gentle.backend.ir.codegen;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.IkeaParentBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.BoxScheme;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaAdd;
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
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovStore;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovStoreEx;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMul;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNeg;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPhi;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaProj;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaRet;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaShl;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaShr;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaShrs;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaSub;
import com.github.firmwehr.gentle.backend.ir.register.Belady;
import com.github.firmwehr.gentle.backend.ir.register.CalleeSavedPrepare;
import com.github.firmwehr.gentle.backend.ir.register.ConstraintNodePrepare;
import com.github.firmwehr.gentle.backend.ir.register.ControlFlowGraph;
import com.github.firmwehr.gentle.backend.ir.register.Dominance;
import com.github.firmwehr.gentle.backend.ir.register.LifetimeAnalysis;
import com.github.firmwehr.gentle.backend.ir.register.LowerPerms;
import com.github.firmwehr.gentle.backend.ir.register.PerfectElimationOrderColorer;
import com.github.firmwehr.gentle.backend.ir.register.Spillprepare;
import com.github.firmwehr.gentle.backend.ir.register.Uses;
import com.github.firmwehr.gentle.backend.ir.register.X86Register;
import com.github.firmwehr.gentle.firm.Util;
import com.github.firmwehr.gentle.firm.model.LoopTree;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.GraphDumper;
import com.github.firmwehr.gentle.util.Mut;
import firm.BackEdges;
import firm.Graph;
import firm.MethodType;
import firm.Mode;
import firm.Relation;
import firm.nodes.Add;
import firm.nodes.Address;
import firm.nodes.Block;
import firm.nodes.Call;
import firm.nodes.Cmp;
import firm.nodes.Cond;
import firm.nodes.Const;
import firm.nodes.Conv;
import firm.nodes.Div;
import firm.nodes.End;
import firm.nodes.Jmp;
import firm.nodes.Load;
import firm.nodes.Minus;
import firm.nodes.Mod;
import firm.nodes.Mul;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize.ILLEGAL;
import static com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize.forMode;

public class CodeSelection extends NodeVisitor.Default {

	private static final Logger LOGGER = new Logger(CodeSelection.class, Logger.LogLevel.DEBUG);

	private final Map<Block, IkeaBløck> blocks;
	private final Map<Block, List<Phi>> phiBär;
	private final Map<Node, IkeaNode> nodes;

	private final Graph graph;
	private final IkeaGraph ikeaGraph;
	private final CodePreselection preselection;

	public CodeSelection(Graph graph, CodePreselection preselection) {
		this.graph = graph;
		this.preselection = preselection;

		this.ikeaGraph = new IkeaGraph();
		this.blocks = new HashMap<>();
		this.phiBär = new HashMap<>();
		this.nodes = new HashMap<>();
	}

	public List<IkeaBløck> convertBlocks() {
		LOGGER.info("Converting blocks for %s", graph.getEntity().getLdName());
		CriticalEdges.breakCriticalEdges(graph);
		GraphDumper.dumpGraph(graph, "critical-edges");

		BackEdges.enable(graph);

		// prepopulate block mappings
		graph.walkBlocks(block -> blocks.put(block, new IkeaBløck(new ArrayList<>(), new ArrayList<>(), block)));

		// collect phis
		graph.walkTopological(new Default() {
			@Override
			public void visit(Phi node) {
				// skip memory phis
				if (node.getMode().equals(Mode.getM())) {
					return;
				}

				IkeaBløck block = blocks.get((Block) node.getBlock());
				IkeaPhi ikeaPhi = new IkeaPhi(ikeaGraph.nextId(), block, ikeaGraph, forMode(node), List.of(node));
				nodes.put(node, ikeaPhi);
				block.nodes().add(ikeaPhi);
				ikeaGraph.addNode(ikeaPhi, List.of());
				phiBär.computeIfAbsent((Block) node.getBlock(), ignore -> new ArrayList<>()).add(node);
			}
		});

		// generate instructions
		graph.walkTopological(this);

		// link phis
		graph.walkBlocks(block -> {
			for (int i = 0, c = block.getPredCount(); i < c; i++) {
				Block pred = (Block) block.getPred(i).getBlock();
				IkeaBløck ikeaPred = blocks.get(pred);
				Set<IkeaNode> parentNodes = new HashSet<>();

				for (Phi phi : phiBär.getOrDefault(block, List.of())) {
					IkeaNode ikeaNode = nodes.get(phi.getPred(i));
					parentNodes.add(ikeaNode);
				}

				blocks.get(block).parents().add(new IkeaParentBløck(ikeaPred, parentNodes));
			}
		});
		BackEdges.disable(graph);

		// Move jmp / return to end of block
		for (IkeaBløck block : blocks.values()) {
			List<IkeaNode> jumps = block.nodes()
				.stream()
				.filter(it -> it instanceof IkeaJcc || it instanceof IkeaJmp || it instanceof IkeaRet ||
					it instanceof IkeaCmp)
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

		// 0. Setup magic stuff
		ControlFlowGraph controlFlowGraph = ControlFlowGraph.forBlocks(orderedBlocks);
		Dominance dominance = Dominance.forCfg(controlFlowGraph);
		LifetimeAnalysis liveliness = new LifetimeAnalysis(controlFlowGraph);
		Uses uses = new Uses(controlFlowGraph, ikeaGraph);
		LoopTree loopTree = LoopTree.forGraph(graph);

		GraphDumper.dumpGraph(controlFlowGraph, "backend-init");

		CalleeSavedPrepare calleeSavedPrepare = new CalleeSavedPrepare(ikeaGraph, controlFlowGraph);
		calleeSavedPrepare.prepare();

		GraphDumper.dumpGraph(controlFlowGraph, "backend-calleeprepare");

		// 1. Prepare for spilling
		Spillprepare prepare = new Spillprepare(liveliness, dominance, uses);
		prepare.prepare(controlFlowGraph);

		GraphDumper.dumpGraph(controlFlowGraph, "backend-spillprepare");

		// 2. Spill
		Belady belady = new Belady(dominance, controlFlowGraph, liveliness, uses, loopTree);
		belady.spill(controlFlowGraph);

		GraphDumper.dumpGraph(controlFlowGraph, "backend-spill");

		// 3. Constraint handling
		ConstraintNodePrepare constraintNodePrepare = new ConstraintNodePrepare(liveliness, uses, dominance);
		constraintNodePrepare.prepare(controlFlowGraph);

		// 4. Color
		PerfectElimationOrderColorer colorer =
			new PerfectElimationOrderColorer(controlFlowGraph, liveliness, dominance, uses);
		colorer.colorGraph();

		// 5. Profit

		LOGGER.error("FANCY! Done for %s", graph.getEntity().getLdName());

		GraphDumper.dumpGraph(controlFlowGraph, "backend-color");

		new LowerPerms(ikeaGraph, controlFlowGraph).lower();

		GraphDumper.dumpGraph(controlFlowGraph, "lower-perms");

		return orderedBlocks;
	}

	private Mut<Optional<X86Register>> noReg() {
		return new Mut<>(Optional.empty());
	}

	@Override
	public void visit(Add node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaAdd ikeaAdd =
			new IkeaAdd(ikeaGraph.nextId(), block, ikeaGraph, forMode(node), List.of(node.getLeft(), node.getRight()));
		nodes.put(node, ikeaAdd);
		block.nodes().add(ikeaAdd);
		ikeaGraph.addNode(ikeaAdd, List.of(nodes.get(node.getLeft()), nodes.get(node.getRight())));
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
		if (node.getMode().equals(Mode.getM())) {
			return;
		}
		IkeaPhi ikeaPhi = (IkeaPhi) nodes.get(node);
		ikeaGraph.overwritePhi(ikeaPhi, Util.predsStream(node).map(nodes::get).toList());
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

		MethodType type = (MethodType) ((Address) node.getPtr()).getEntity().getType();
		IkeaRegisterSize size = ILLEGAL;
		if (type.getNRess() > 0 && !type.getResType(0).getMode().equals(Mode.getANY())) {
			size = forMode(type.getResType(0).getMode());
		}

		IkeaCall ikeaCall = new IkeaCall(ikeaGraph.nextId(), block, ikeaGraph, size, List.of(node));
		ikeaGraph.addNode(ikeaCall, arguments);
		nodes.put(node, ikeaCall);
		block.nodes().add(ikeaCall);
	}

	@Override
	public void visit(Cmp node) {
		// x86 uses special registers for conditional jump, so cmp result needs to be preceding conditional jump
	}

	private IkeaCmp visitFromCond(Cmp node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());
		return new IkeaCmp(ikeaGraph.nextId(), block, ikeaGraph, List.of(node));
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

		IkeaCmp cmp = visitFromCond((Cmp) node.getSelector());

		IkeaJcc ikeaJcc =
			new IkeaJcc(ikeaGraph.nextId(), block, ikeaGraph, List.of(node), relation, blocks.get(trueBlock),
				blocks.get(falseBlock));
		this.nodes.put(node, ikeaJcc);
		block.nodes().add(cmp);
		block.nodes().add(ikeaJcc);
		ikeaGraph.addNode(cmp, List.of(this.nodes.get(((Cmp) node.getSelector()).getLeft()),
			this.nodes.get(((Cmp) node.getSelector()).getRight())));
		ikeaGraph.addNode(ikeaJcc, List.of(cmp));
	}

	@Override
	public void visit(Const node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaConst ikeaConst =
			new IkeaConst(ikeaGraph.nextId(), block, ikeaGraph, forMode(node), List.of(node), node.getTarval());
		nodes.put(node, ikeaConst);
		block.nodes().add(ikeaConst);
		ikeaGraph.addNode(ikeaConst, List.of());
	}

	@Override
	public void visit(Conv node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaConv ikeaConv = new IkeaConv(ikeaGraph.nextId(), block, ikeaGraph, forMode(node), List.of(node));
		nodes.put(node, ikeaConv);
		block.nodes().add(ikeaConv);
		ikeaGraph.addNode(ikeaConv, List.of(nodes.get(node.getOp())));
	}

	@Override
	public void visit(Div node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaDiv ikeaDiv = new IkeaDiv(ikeaGraph.nextId(), block, ikeaGraph, forMode(node.getResmode()), List.of(node));
		nodes.put(node, ikeaDiv);
		block.nodes().add(ikeaDiv);
		ikeaGraph.addNode(ikeaDiv, List.of(nodes.get(node.getLeft()), nodes.get(node.getRight())));

		IkeaProj divProj =
			new IkeaProj(ikeaGraph.nextId(), block, ikeaGraph, forMode(node.getResmode()), List.of(node), 0, "div");
		block.nodes().add(divProj);
		nodes.put(node, divProj);
		ikeaGraph.addNode(divProj, List.of(ikeaDiv));
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
				IkeaJmp ikeaJmp =
					new IkeaJmp(ikeaGraph.nextId(), block, ikeaGraph, List.of(node), blocks.get(targetBlock));
				nodes.put(node, ikeaJmp);
				block.nodes().add(ikeaJmp);
				ikeaGraph.addNode(ikeaJmp, List.of());
			}
		}
	}

	@Override
	public void visit(Load node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());

		var maybeScheme = preselection.scheme(node);
		if (maybeScheme.isPresent()) {
			var scheme = maybeScheme.get();

			IkeaMovLoadEx mov =
				new IkeaMovLoadEx(ikeaGraph.nextId(), block, ikeaGraph, forMode(node.getLoadMode()), List.of(node),
					BoxScheme.fromAddressingScheme(scheme, nodes::get));
			nodes.put(node, mov);
			block.nodes().add(mov);
			// FIXME: This mess
			ikeaGraph.addNode(mov, List.of());
			return;
		}

		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaMovLoad ikeaMovLoad =
			new IkeaMovLoad(ikeaGraph.nextId(), block, ikeaGraph, forMode(node.getLoadMode()), List.of(node));
		nodes.put(node, ikeaMovLoad);
		block.nodes().add(ikeaMovLoad);
		ikeaGraph.addNode(ikeaMovLoad, List.of(nodes.get(node.getPtr())));
	}

	@Override
	public void visit(Minus node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaNeg ikeaNeg = new IkeaNeg(ikeaGraph.nextId(), block, ikeaGraph, forMode(node), List.of(node));
		nodes.put(node, ikeaNeg);
		block.nodes().add(ikeaNeg);
		ikeaGraph.addNode(ikeaNeg, List.of(nodes.get(node.getOp())));
	}

	@Override
	public void visit(Mod node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaDiv ikeaDiv = new IkeaDiv(ikeaGraph.nextId(), block, ikeaGraph, forMode(node.getResmode()), List.of(node));
		nodes.put(node, ikeaDiv);
		block.nodes().add(ikeaDiv);
		ikeaGraph.addNode(ikeaDiv, List.of(nodes.get(node.getLeft()), nodes.get(node.getRight())));

		IkeaProj modProj =
			new IkeaProj(ikeaGraph.nextId(), block, ikeaGraph, forMode(node.getResmode()), List.of(node), 1, "mod");
		nodes.put(node, modProj);
		block.nodes().add(modProj);
		ikeaGraph.addNode(modProj, List.of(ikeaDiv));
	}

	@Override
	public void visit(Mul node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaMul ikeaMul = new IkeaMul(ikeaGraph.nextId(), block, ikeaGraph, forMode(node), List.of(node));
		nodes.put(node, ikeaMul);
		block.nodes().add(ikeaMul);
		ikeaGraph.addNode(ikeaMul, List.of(nodes.get(node.getLeft()), nodes.get(node.getRight())));
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
		IkeaArgNode ikeaArgNode =
			new IkeaArgNode(ikeaGraph.nextId(), block, ikeaGraph, forMode(proj), List.of(proj), proj.getNum());
		nodes.put(proj, ikeaArgNode);
		block.nodes().add(ikeaArgNode);
		ikeaGraph.addNode(ikeaArgNode, List.of());
	}

	@Override
	public void visit(Return node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaRet ikeaRet = new IkeaRet(ikeaGraph.nextId(), block, ikeaGraph, List.of(node));
		nodes.put(node, ikeaRet);
		block.nodes().add(ikeaRet);
		ikeaGraph.addNode(ikeaRet,
			Util.predsStream(node).filter(n -> !n.getMode().equals(Mode.getM())).limit(1).map(nodes::get).toList());
	}

	@Override
	public void visit(Store node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());

		var value = nodes.get(node.getValue());

		var maybeScheme = preselection.scheme(node);
		if (maybeScheme.isPresent()) {
			var scheme = maybeScheme.get();

			IkeaMovStoreEx mov = new IkeaMovStoreEx(ikeaGraph.nextId(), block, ikeaGraph, List.of(node),
				BoxScheme.fromAddressingScheme(scheme, nodes::get));
			nodes.put(node, mov);
			block.nodes().add(mov);
			// FIXME: This mess
			ikeaGraph.addNode(value, List.of());
			return;
		}

		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaMovStore ikeaMovStore = new IkeaMovStore(ikeaGraph.nextId(), block, ikeaGraph, List.of(node));
		nodes.put(node, ikeaMovStore);
		block.nodes().add(ikeaMovStore);
		ikeaGraph.addNode(ikeaMovStore, List.of(nodes.get(node.getPtr()), value));
	}

	@Override
	public void visit(Shl node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaShl ikeaShl = new IkeaShl(ikeaGraph.nextId(), block, ikeaGraph, forMode(node), List.of(node));
		nodes.put(node, ikeaShl);
		block.nodes().add(ikeaShl);
		ikeaGraph.addNode(ikeaShl, List.of(nodes.get(node.getLeft()), nodes.get(node.getRight())));
	}

	@Override
	public void visit(Shr node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaShr ikeaShr = new IkeaShr(ikeaGraph.nextId(), block, ikeaGraph, forMode(node), List.of(node));
		nodes.put(node, ikeaShr);
		block.nodes().add(ikeaShr);
		ikeaGraph.addNode(ikeaShr, List.of(nodes.get(node.getLeft()), nodes.get(node.getRight())));
	}

	@Override
	public void visit(Shrs node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaShrs ikeaShrs = new IkeaShrs(ikeaGraph.nextId(), block, ikeaGraph, forMode(node), List.of(node));
		nodes.put(node, ikeaShrs);
		block.nodes().add(ikeaShrs);
		ikeaGraph.addNode(ikeaShrs, List.of(nodes.get(node.getLeft()), nodes.get(node.getRight())));
	}

	@Override
	public void visit(Sub node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaSub ikeaSub = new IkeaSub(ikeaGraph.nextId(), block, ikeaGraph, forMode(node), List.of(node));
		nodes.put(node, ikeaSub);
		block.nodes().add(ikeaSub);
		ikeaGraph.addNode(ikeaSub, List.of(nodes.get(node.getLeft()), nodes.get(node.getRight())));
	}

	@Override
	public void visit(Unknown node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaConst ikeaConst =
			new IkeaConst(ikeaGraph.nextId(), block, ikeaGraph, forMode(node), List.of(node),
				node.getMode().getNull());
		nodes.put(node, ikeaConst);
		block.nodes().add(ikeaConst);
		ikeaGraph.addNode(ikeaConst, List.of());
	}

	@Override
	public void defaultVisit(Node n) {
		throw new InternalCompilerException("Unexpected node " + n);
	}
}
