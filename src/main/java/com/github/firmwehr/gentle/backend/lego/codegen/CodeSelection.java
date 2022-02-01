package com.github.firmwehr.gentle.backend.lego.codegen;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.lego.LegoBøx.LegoRegisterSize;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.LegoParentBløck;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.nodes.BoxScheme;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoAdd;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoArgNode;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoCall;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoCmp;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoConst;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoConv;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoDiv;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoJcc;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoJmp;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovLoad;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovLoadEx;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovStore;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovStoreEx;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMul;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNeg;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoPhi;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoProj;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoRet;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoShl;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoShr;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoShrs;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoSub;
import com.github.firmwehr.gentle.backend.lego.register.Belady;
import com.github.firmwehr.gentle.backend.lego.register.CalleeSavedPrepare;
import com.github.firmwehr.gentle.backend.lego.register.ConstraintNodePrepare;
import com.github.firmwehr.gentle.backend.lego.register.ControlFlowGraph;
import com.github.firmwehr.gentle.backend.lego.register.Dominance;
import com.github.firmwehr.gentle.backend.lego.register.LifetimeAnalysis;
import com.github.firmwehr.gentle.backend.lego.register.LowerPerms;
import com.github.firmwehr.gentle.backend.lego.register.PerfectElimationOrderColorer;
import com.github.firmwehr.gentle.backend.lego.register.Spillprepare;
import com.github.firmwehr.gentle.backend.lego.register.Uses;
import com.github.firmwehr.gentle.backend.lego.register.X86Register;
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

import static com.github.firmwehr.gentle.backend.lego.LegoBøx.LegoRegisterSize.ILLEGAL;
import static com.github.firmwehr.gentle.backend.lego.LegoBøx.LegoRegisterSize.forMode;

public class CodeSelection extends NodeVisitor.Default {

	private static final Logger LOGGER = new Logger(CodeSelection.class, Logger.LogLevel.DEBUG);

	private final Map<Block, LegoPlate> blocks;
	private final Map<Block, List<Phi>> phiBär;
	private final Map<Node, LegoNode> nodes;

	private final Graph graph;
	private final LegoGraph legoGraph;
	private final CodePreselection preselection;

	public CodeSelection(Graph graph, CodePreselection preselection) {
		this.graph = graph;
		this.preselection = preselection;

		this.legoGraph = new LegoGraph();
		this.blocks = new HashMap<>();
		this.phiBär = new HashMap<>();
		this.nodes = new HashMap<>();
	}

	public List<LegoPlate> convertBlocks() {
		LOGGER.info("Converting blocks for %s", graph.getEntity().getLdName());
		CriticalEdges.breakCriticalEdges(graph);
		GraphDumper.dumpGraph(graph, "critical-edges");

		BackEdges.enable(graph);

		// prepopulate block mappings
		graph.walkBlocks(block -> blocks.put(block, new LegoPlate(new ArrayList<>(), new ArrayList<>(), block)));

		// collect phis
		graph.walkTopological(new Default() {
			@Override
			public void visit(Phi node) {
				// skip memory phis
				if (node.getMode().equals(Mode.getM())) {
					return;
				}

				LegoPlate block = blocks.get((Block) node.getBlock());
				LegoPhi legoPhi = new LegoPhi(legoGraph.nextId(), block, legoGraph, forMode(node), List.of(node));
				nodes.put(node, legoPhi);
				block.nodes().add(legoPhi);
				legoGraph.addNode(legoPhi, List.of());
				phiBär.computeIfAbsent((Block) node.getBlock(), ignore -> new ArrayList<>()).add(node);
			}
		});

		// generate instructions
		graph.walkTopological(this);

		// link phis
		graph.walkBlocks(block -> {
			for (int i = 0, c = block.getPredCount(); i < c; i++) {
				Block pred = (Block) block.getPred(i).getBlock();
				LegoPlate legoPred = blocks.get(pred);
				Set<LegoNode> parentNodes = new HashSet<>();

				for (Phi phi : phiBär.getOrDefault(block, List.of())) {
					LegoNode legoNode = nodes.get(phi.getPred(i));
					parentNodes.add(legoNode);
				}

				blocks.get(block).parents().add(new LegoParentBløck(legoPred, parentNodes));
			}
		});
		BackEdges.disable(graph);

		// Move jmp / return to end of block
		for (LegoPlate block : blocks.values()) {
			List<LegoNode> jumps = block.nodes()
				.stream()
				.filter(it -> it instanceof LegoJcc || it instanceof LegoJmp || it instanceof LegoRet ||
					it instanceof LegoCmp)
				.toList();
			block.nodes().removeAll(jumps);
			block.nodes().addAll(jumps);
		}

		List<LegoPlate> orderedBlocks = blocks.values()
			.stream()
			.filter(it -> !it.origin().equals(graph.getEndBlock()))
			.collect(Collectors.toCollection(ArrayList::new));
		orderedBlocks.remove(blocks.get(graph.getStartBlock()));
		orderedBlocks.add(0, blocks.get(graph.getStartBlock()));

		for (LegoPlate block : orderedBlocks) {
			List<LegoNode> legoNodes = block.nodes();
			for (int nodeIndex = 0; nodeIndex < legoNodes.size(); nodeIndex++) {
				LegoNode node = legoNodes.get(nodeIndex);
				List<LegoNode> inputs = node.inputs();
				for (int inputIndex = 0; inputIndex < inputs.size(); inputIndex++) {
					LegoNode input = inputs.get(inputIndex);

					if (input instanceof LegoConst constNode) {
						LegoConst newConst = new LegoConst(legoGraph.nextId(), block, legoGraph, input.size(),
							input.underlyingFirmNodes(), constNode.value());
						legoGraph.addNode(newConst, List.of());
						legoGraph.setInput(node, inputIndex, newConst);
						block.nodes().add(nodeIndex, newConst);
						nodeIndex++;
					}
				}
			}
		}

		List<LegoNode> argNodes = new ArrayList<>();
		for (LegoPlate block : orderedBlocks) {
			block.nodes().removeIf(node -> legoGraph.getOutputEdges(node).isEmpty() && node instanceof LegoConst);
			List<LegoNode> myArgNodes = block.nodes().stream().filter(it -> it instanceof LegoArgNode).toList();
			block.nodes().removeAll(myArgNodes);
			argNodes.addAll(myArgNodes);
		}
		orderedBlocks.get(0).nodes().addAll(0, argNodes);

		// 0. Setup magic stuff
		ControlFlowGraph controlFlowGraph = ControlFlowGraph.forBlocks(orderedBlocks);
		Dominance dominance = Dominance.forCfg(controlFlowGraph);
		LifetimeAnalysis liveliness = new LifetimeAnalysis(controlFlowGraph);
		Uses uses = new Uses(controlFlowGraph, legoGraph);
		LoopTree loopTree = LoopTree.forGraph(graph);

		GraphDumper.dumpGraph(controlFlowGraph, "backend-init");

		CalleeSavedPrepare calleeSavedPrepare = new CalleeSavedPrepare(legoGraph, controlFlowGraph);
		calleeSavedPrepare.prepare();
		liveliness.recompute();
		dominance.recompute();

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

		GraphDumper.dumpGraph(controlFlowGraph, "backend-constrprepare");

		// 4. Color
		PerfectElimationOrderColorer colorer =
			new PerfectElimationOrderColorer(controlFlowGraph, liveliness, dominance, uses);
		colorer.colorGraph();

		// 5. Profit

		LOGGER.error("FANCY! Done for %s", graph.getEntity().getLdName());

		GraphDumper.dumpGraph(controlFlowGraph, "backend-color");

		new LowerPerms(legoGraph, controlFlowGraph).lower();

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

		LegoPlate block = blocks.get((Block) node.getBlock());
		LegoAdd legoAdd =
			new LegoAdd(legoGraph.nextId(), block, legoGraph, forMode(node), List.of(node.getLeft(), node.getRight()));
		nodes.put(node, legoAdd);
		block.nodes().add(legoAdd);
		legoGraph.addNode(legoAdd, List.of(nodes.get(node.getLeft()), nodes.get(node.getRight())));
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
		LegoPhi legoPhi = (LegoPhi) nodes.get(node);
		legoGraph.overwritePhi(legoPhi, Util.predsStream(node).map(nodes::get).toList());
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

		LegoPlate block = blocks.get((Block) node.getBlock());
		List<LegoNode> arguments = Util.predsStream(node)
			.filter(it -> !it.getMode().equals(Mode.getM()))
			.filter(it -> !(it instanceof Address))
			.map(nodes::get)
			.toList();

		MethodType type = (MethodType) ((Address) node.getPtr()).getEntity().getType();
		LegoRegisterSize size = ILLEGAL;
		if (type.getNRess() > 0 && !type.getResType(0).getMode().equals(Mode.getANY())) {
			size = forMode(type.getResType(0).getMode());
		}

		LegoCall legoCall = new LegoCall(legoGraph.nextId(), block, legoGraph, size, List.of(node));
		legoGraph.addNode(legoCall, arguments);
		nodes.put(node, legoCall);
		block.nodes().add(legoCall);
	}

	@Override
	public void visit(Cmp node) {
		// x86 uses special registers for conditional jump, so cmp result needs to be preceding conditional jump
	}

	private LegoCmp visitFromCond(Cmp node) {
		LegoPlate block = blocks.get((Block) node.getBlock());
		return new LegoCmp(legoGraph.nextId(), block, legoGraph, List.of(node));
	}

	@Override
	public void visit(Cond node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		LegoPlate block = blocks.get((Block) node.getBlock());
		List<Node> nodes = Util.outsStream(node).toList();
		Node trueProj = ((Proj) nodes.get(0)).getNum() == Cond.pnTrue ? nodes.get(0) : nodes.get(1);
		Node falseProj = ((Proj) nodes.get(0)).getNum() == Cond.pnFalse ? nodes.get(0) : nodes.get(1);

		BackEdges.Edge trueEdge = BackEdges.getOuts(trueProj).iterator().next();
		Block trueBlock = (Block) trueEdge.node;
		BackEdges.Edge falseEdge = BackEdges.getOuts(falseProj).iterator().next();
		Block falseBlock = (Block) falseEdge.node;

		Relation relation = ((Cmp) node.getSelector()).getRelation();

		LegoCmp cmp = visitFromCond((Cmp) node.getSelector());

		LegoJcc legoJcc =
			new LegoJcc(legoGraph.nextId(), block, legoGraph, List.of(node), relation, blocks.get(trueBlock),
				blocks.get(falseBlock));
		this.nodes.put(node, legoJcc);
		block.nodes().add(cmp);
		block.nodes().add(legoJcc);
		legoGraph.addNode(cmp, List.of(this.nodes.get(((Cmp) node.getSelector()).getLeft()),
			this.nodes.get(((Cmp) node.getSelector()).getRight())));
		legoGraph.addNode(legoJcc, List.of(cmp));
	}

	@Override
	public void visit(Const node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		LegoPlate block = blocks.get((Block) node.getBlock());
		LegoConst legoConst =
			new LegoConst(legoGraph.nextId(), block, legoGraph, forMode(node), List.of(node), node.getTarval());
		nodes.put(node, legoConst);
		block.nodes().add(legoConst);
		legoGraph.addNode(legoConst, List.of());
	}

	@Override
	public void visit(Conv node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		LegoPlate block = blocks.get((Block) node.getBlock());
		LegoConv legoConv = new LegoConv(legoGraph.nextId(), block, legoGraph, forMode(node), List.of(node));
		nodes.put(node, legoConv);
		block.nodes().add(legoConv);
		legoGraph.addNode(legoConv, List.of(nodes.get(node.getOp())));
	}

	@Override
	public void visit(Div node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		LegoPlate block = blocks.get((Block) node.getBlock());
		LegoDiv legoDiv = new LegoDiv(legoGraph.nextId(), block, legoGraph, forMode(node.getResmode()), List.of(node));
		nodes.put(node, legoDiv);
		block.nodes().add(legoDiv);
		legoGraph.addNode(legoDiv, List.of(nodes.get(node.getLeft()), nodes.get(node.getRight())));

		LegoProj divProj =
			new LegoProj(legoGraph.nextId(), block, legoGraph, forMode(node.getResmode()), List.of(node), 0, "div");
		block.nodes().add(divProj);
		nodes.put(node, divProj);
		legoGraph.addNode(divProj, List.of(legoDiv));
	}

	@Override
	public void visit(Jmp node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		LegoPlate block = blocks.get((Block) node.getBlock());
		for (BackEdges.Edge edge : BackEdges.getOuts(node)) {
			if (edge.node instanceof Block targetBlock) {
				LegoJmp legoJmp =
					new LegoJmp(legoGraph.nextId(), block, legoGraph, List.of(node), blocks.get(targetBlock));
				nodes.put(node, legoJmp);
				block.nodes().add(legoJmp);
				legoGraph.addNode(legoJmp, List.of());
			}
		}
	}

	@Override
	public void visit(Load node) {
		LegoPlate block = blocks.get((Block) node.getBlock());

		var maybeScheme = preselection.scheme(node);
		if (maybeScheme.isPresent()) {
			var scheme = maybeScheme.get();

			LegoMovLoadEx mov =
				new LegoMovLoadEx(legoGraph.nextId(), block, legoGraph, forMode(node.getLoadMode()), List.of(node),
					BoxScheme.fromAddressingScheme(scheme, nodes::get));
			nodes.put(node, mov);
			block.nodes().add(mov);
			// FIXME: This mess
			legoGraph.addNode(mov, List.of());
			return;
		}

		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		LegoMovLoad legoMovLoad =
			new LegoMovLoad(legoGraph.nextId(), block, legoGraph, forMode(node.getLoadMode()), List.of(node));
		nodes.put(node, legoMovLoad);
		block.nodes().add(legoMovLoad);
		legoGraph.addNode(legoMovLoad, List.of(nodes.get(node.getPtr())));
	}

	@Override
	public void visit(Minus node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		LegoPlate block = blocks.get((Block) node.getBlock());
		LegoNeg legoNeg = new LegoNeg(legoGraph.nextId(), block, legoGraph, forMode(node), List.of(node));
		nodes.put(node, legoNeg);
		block.nodes().add(legoNeg);
		legoGraph.addNode(legoNeg, List.of(nodes.get(node.getOp())));
	}

	@Override
	public void visit(Mod node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		LegoPlate block = blocks.get((Block) node.getBlock());
		LegoDiv legoDiv = new LegoDiv(legoGraph.nextId(), block, legoGraph, forMode(node.getResmode()), List.of(node));
		nodes.put(node, legoDiv);
		block.nodes().add(legoDiv);
		legoGraph.addNode(legoDiv, List.of(nodes.get(node.getLeft()), nodes.get(node.getRight())));

		LegoProj modProj =
			new LegoProj(legoGraph.nextId(), block, legoGraph, forMode(node.getResmode()), List.of(node), 1, "mod");
		nodes.put(node, modProj);
		block.nodes().add(modProj);
		legoGraph.addNode(modProj, List.of(legoDiv));
	}

	@Override
	public void visit(Mul node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		LegoPlate block = blocks.get((Block) node.getBlock());
		LegoMul legoMul = new LegoMul(legoGraph.nextId(), block, legoGraph, forMode(node), List.of(node));
		nodes.put(node, legoMul);
		block.nodes().add(legoMul);
		legoGraph.addNode(legoMul, List.of(nodes.get(node.getLeft()), nodes.get(node.getRight())));
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

		LegoPlate block = blocks.get((Block) proj.getBlock());
		LegoArgNode legoArgNode =
			new LegoArgNode(legoGraph.nextId(), block, legoGraph, forMode(proj), List.of(proj), proj.getNum());
		nodes.put(proj, legoArgNode);
		block.nodes().add(legoArgNode);
		legoGraph.addNode(legoArgNode, List.of());
	}

	@Override
	public void visit(Return node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		LegoPlate block = blocks.get((Block) node.getBlock());
		LegoRet legoRet = new LegoRet(legoGraph.nextId(), block, legoGraph, List.of(node));
		nodes.put(node, legoRet);
		block.nodes().add(legoRet);
		legoGraph.addNode(legoRet,
			Util.predsStream(node).filter(n -> !n.getMode().equals(Mode.getM())).limit(1).map(nodes::get).toList());
	}

	@Override
	public void visit(Store node) {
		LegoPlate block = blocks.get((Block) node.getBlock());

		var value = nodes.get(node.getValue());

		var maybeScheme = preselection.scheme(node);
		if (maybeScheme.isPresent()) {
			var scheme = maybeScheme.get();

			LegoMovStoreEx mov = new LegoMovStoreEx(legoGraph.nextId(), block, legoGraph, List.of(node),
				BoxScheme.fromAddressingScheme(scheme, nodes::get));
			nodes.put(node, mov);
			block.nodes().add(mov);
			// FIXME: This mess
			legoGraph.addNode(mov, List.of());
			return;
		}

		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		LegoMovStore legoMovStore = new LegoMovStore(legoGraph.nextId(), block, legoGraph, List.of(node));
		nodes.put(node, legoMovStore);
		block.nodes().add(legoMovStore);
		legoGraph.addNode(legoMovStore, List.of(nodes.get(node.getPtr()), value));
	}

	@Override
	public void visit(Shl node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		LegoPlate block = blocks.get((Block) node.getBlock());
		LegoShl legoShl = new LegoShl(legoGraph.nextId(), block, legoGraph, forMode(node), List.of(node));
		nodes.put(node, legoShl);
		block.nodes().add(legoShl);
		legoGraph.addNode(legoShl, List.of(nodes.get(node.getLeft()), nodes.get(node.getRight())));
	}

	@Override
	public void visit(Shr node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		LegoPlate block = blocks.get((Block) node.getBlock());
		LegoShr legoShr = new LegoShr(legoGraph.nextId(), block, legoGraph, forMode(node), List.of(node));
		nodes.put(node, legoShr);
		block.nodes().add(legoShr);
		legoGraph.addNode(legoShr, List.of(nodes.get(node.getLeft()), nodes.get(node.getRight())));
	}

	@Override
	public void visit(Shrs node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		LegoPlate block = blocks.get((Block) node.getBlock());
		LegoShrs legoShrs = new LegoShrs(legoGraph.nextId(), block, legoGraph, forMode(node), List.of(node));
		nodes.put(node, legoShrs);
		block.nodes().add(legoShrs);
		legoGraph.addNode(legoShrs, List.of(nodes.get(node.getLeft()), nodes.get(node.getRight())));
	}

	@Override
	public void visit(Sub node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		LegoPlate block = blocks.get((Block) node.getBlock());
		LegoSub legoSub = new LegoSub(legoGraph.nextId(), block, legoGraph, forMode(node), List.of(node));
		nodes.put(node, legoSub);
		block.nodes().add(legoSub);
		legoGraph.addNode(legoSub, List.of(nodes.get(node.getLeft()), nodes.get(node.getRight())));
	}

	@Override
	public void visit(Unknown node) {
		// skip node if code selection has replaced it with better x86 specific op
		if (preselection.hasBeenReplaced(node)) {
			return;
		}

		LegoPlate block = blocks.get((Block) node.getBlock());
		LegoConst legoConst =
			new LegoConst(legoGraph.nextId(), block, legoGraph, forMode(node), List.of(node),
				node.getMode().getNull());
		nodes.put(node, legoConst);
		block.nodes().add(legoConst);
		legoGraph.addNode(legoConst, List.of());
	}

	@Override
	public void defaultVisit(Node n) {
		throw new InternalCompilerException("Unexpected node " + n);
	}
}
