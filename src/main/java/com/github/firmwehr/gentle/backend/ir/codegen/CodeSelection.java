package com.github.firmwehr.gentle.backend.ir.codegen;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaImmediate;
import com.github.firmwehr.gentle.backend.ir.IkeaParentBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaVirtualRegister;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaAdd;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaCall;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaCmp;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaConst;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaConv;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaJcc;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaJmp;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovLoad;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovStore;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMul;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNeg;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPhi;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaRet;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaSub;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.nodes.Add;
import firm.nodes.Address;
import firm.nodes.Block;
import firm.nodes.Call;
import firm.nodes.Cmp;
import firm.nodes.Cond;
import firm.nodes.Const;
import firm.nodes.Conv;
import firm.nodes.Jmp;
import firm.nodes.Load;
import firm.nodes.Minus;
import firm.nodes.Mul;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Phi;
import firm.nodes.Proj;
import firm.nodes.Return;
import firm.nodes.Store;
import firm.nodes.Sub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

public class CodeSelection extends NodeVisitor.Default {

	private final Map<Block, IkeaBløck> blocks;
	private final Map<Block, List<Phi>> phiBär;
	private final Map<Node, IkeaNode> nodes;
	private final Graph graph;
	private int regCount;

	public CodeSelection(Graph graph) {
		this.graph = graph;
		this.blocks = new HashMap<>();
		this.phiBär = new HashMap<>();
		this.nodes = new HashMap<>();
		this.regCount = this.graph.getEntity().getParameterNumber();
	}

	public void convertBlocks() {
		BackEdges.enable(graph);
		graph.walkBlocks(block -> blocks.put(block, new IkeaBløck(new ArrayList<>(), new ArrayList<>(), block)));
		graph.walkTopological(this);
		graph.walkBlocks(block -> {
			for (int i = 0, c = block.getPredCount(); i < c; i++) {
				Block pred = (Block) block.getPred(i).getBlock();
				Map<IkeaBøx, IkeaBøx> renames = new HashMap<>();
				for (Phi phi : phiBär.get(block)) {
					IkeaNode ikeaNode = nodes.get(phi.getPred(i));
					IkeaBøx target = nodes.get(phi).box();
					IkeaBøx source = ikeaNode.box();
					renames.put(source, target);
				}
				blocks.get(block).parents().add(new IkeaParentBløck(blocks.get(pred), renames));
			}
		});
		BackEdges.disable(graph);
	}

	private IkeaBøx nextRegister() {
		return new IkeaVirtualRegister(this.regCount++);
	}

	@Override
	public void visit(Add node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaAdd ikeaAdd = new IkeaAdd(nextRegister(), nodes.get(node.getLeft()), nodes.get(node.getRight()), node);
		nodes.put(node, ikeaAdd);
		block.nodes().add(ikeaAdd);
	}

	@Override
	public void visit(Call node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaCall ikeaCall =
			new IkeaCall(nextRegister(), (Address) node.getPred(1/*not a magic value!*/), List.of()/*FIXME*/, node);
		nodes.put(node, ikeaCall);
		block.nodes().add(ikeaCall);
	}

	@Override
	public void visit(Cmp node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaCmp ikeaCmp = new IkeaCmp(nextRegister(), nodes.get(node.getLeft()), nodes.get(node.getRight()), node);
		nodes.put(node, ikeaCmp);
		block.nodes().add(ikeaCmp);
	}

	@Override
	public void visit(Cond node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());
		List<Node> nodes =
			StreamSupport.stream(BackEdges.getOuts(node).spliterator(), false).map(edge -> edge.node).toList();
		Node trueProj = ((Proj) nodes.get(0)).getNum() == Cond.pnTrue ? nodes.get(0) : nodes.get(1);
		Node falseProj = ((Proj) nodes.get(0)).getNum() == Cond.pnFalse ? nodes.get(0) : nodes.get(1);

		BackEdges.Edge trueEdge = BackEdges.getOuts(trueProj).iterator().next();
		Block trueBlock = (Block) trueEdge.node;
		BackEdges.Edge falseEdge = BackEdges.getOuts(falseProj).iterator().next();
		Block falseBlock = (Block) falseEdge.node;
		IkeaJcc ikeaJcc =
			new IkeaJcc(blocks.get(trueBlock), blocks.get(falseBlock), node, ((Cmp) node.getSelector()).getRelation(),
				this.nodes.get(node.getSelector()));
		this.nodes.put(node, ikeaJcc);
		block.nodes().add(ikeaJcc);
	}

	@Override
	public void visit(Const node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaConst ikeaConst = new IkeaConst(new IkeaImmediate(node.getTarval()), node);
		nodes.put(node, ikeaConst);
		block.nodes().add(ikeaConst);
	}

	@Override
	public void visit(Conv node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaConv ikeaConv =
			new IkeaConv(nextRegister(), nodes.get(node.getOp()), node.getOp().getMode(), node.getMode(), node);
		nodes.put(node, ikeaConv);
		block.nodes().add(ikeaConv);
	}

	@Override
	public void visit(Jmp node) {
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
		IkeaMovLoad ikeaMovLoad = new IkeaMovLoad(nextRegister(), nodes.get(node.getPtr()), node);
		nodes.put(node, ikeaMovLoad);
		block.nodes().add(ikeaMovLoad);
	}

	@Override
	public void visit(Minus node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaNeg ikeaNeg = new IkeaNeg(nextRegister(), nodes.get(node.getOp()), node);
		nodes.put(node, ikeaNeg);
		block.nodes().add(ikeaNeg);
	}

	@Override
	public void visit(Mul node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaMul ikeaMul = new IkeaMul(nextRegister(), nodes.get(node.getLeft()), nodes.get(node.getRight()), node);
		nodes.put(node, ikeaMul);
		block.nodes().add(ikeaMul);
	}

	@Override
	public void visit(Phi node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaPhi ikeaPhi = new IkeaPhi(nextRegister(), node);
		nodes.put(node, ikeaPhi);
		block.nodes().add(ikeaPhi);
		this.phiBär.computeIfAbsent((Block) node.getBlock(), ignore -> new ArrayList<>()).add(node);
	}

	@Override
	public void visit(Proj node) {
		super.visit(node);
	}

	@Override
	public void visit(Return node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaRet ikeaRet = new IkeaRet(StreamSupport.stream(node.getPreds().spliterator(), false)
			.filter(n -> !n.getMode().equals(Mode.getM()))
			.findFirst()
			.map(nodes::get), node);
		nodes.put(node, ikeaRet);
		block.nodes().add(ikeaRet);
	}

	@Override
	public void visit(Store node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaMovStore ikeaMovStore = new IkeaMovStore(nextRegister(), nodes.get(node.getPtr()), node);
		nodes.put(node, ikeaMovStore);
		block.nodes().add(ikeaMovStore);
	}

	@Override
	public void visit(Sub node) {
		IkeaBløck block = blocks.get((Block) node.getBlock());
		IkeaSub ikeaSub = new IkeaSub(nextRegister(), nodes.get(node.getLeft()), nodes.get(node.getRight()), node);
		nodes.put(node, ikeaSub);
		block.nodes().add(ikeaSub);
	}
}
