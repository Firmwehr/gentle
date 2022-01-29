package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.fiascii.FiAscii;
import com.github.firmwehr.fiascii.generated.CmpWithSameInputsPattern;
import com.github.firmwehr.fiascii.generated.CondToBoolShortcutPattern;
import com.github.firmwehr.gentle.firm.Util;
import com.github.firmwehr.gentle.output.Logger;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.bindings.binding_irgopt;
import firm.bindings.binding_irnode;
import firm.nodes.Cond;
import firm.nodes.Const;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Phi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.firmwehr.gentle.util.GraphDumper.dumpGraph;

public class BooleanOptimization extends NodeVisitor.Default {

	private static final Logger LOGGER = new Logger(BooleanOptimization.class);

	private static final OptimizationList OPTIMIZATIONS = OptimizationList.builder() //
		.addStep(BooleanOptimization::cmpWithSameInputs, (match, graph, block) -> {
			// The shared input is already const
			if (match.input() instanceof Const) {
				return false;
			}

			Node newInput = graph.newConst(match.input().getMode().getNull());
			match.cmp().setLeft(newInput);
			match.cmp().setRight(newInput);
			return true;
		}) //
		.addStep(BooleanOptimization::condToBoolShortcut, BooleanOptimization::applyCondToBoolShortcut) //
		.build();

	private boolean hasChanged;
	private final Graph graph;

	public BooleanOptimization(Graph graph) {
		this.graph = graph;
	}

	public static GraphOptimizationStep<Graph, Boolean> booleanOptimization() {
		return GraphOptimizationStep.<Graph, Boolean>builder()
			.withDescription("BooleanOptimization")
			.withOptimizationFunction(graph -> {
				int runs = 0;
				while (true) {
					// Needs to be done in each iteration apparently?
					BackEdges.enable(graph);

					BooleanOptimization booleanOptimization = new BooleanOptimization(graph);
					booleanOptimization.applyBooleanOptimization();
					binding_irgopt.remove_bads(graph.ptr);
					binding_irgopt.remove_unreachable_code(graph.ptr);

					// testing has shown that back edges get disabled anyway for some reason, but we don't like
					// problems
					BackEdges.disable(graph);

					if (!booleanOptimization.hasChanged) {
						break;
					} else if (LOGGER.isDebugEnabled()) {
						dumpGraph(graph, "boolean-iteration");
					}
					runs++;
				}
				boolean changed = runs > 0;
				if (changed) {
					dumpGraph(graph, "boolean");
				}
				return changed;
			})
			.build();
	}

	private static boolean applyCondToBoolShortcut(CondToBoolShortcutPattern.Match match, Graph graph, Node block) {
		boolean changed = false;

		for (Phi phi : findPhi(match)) {
			int trueIndex = match.out1().getNum() == Cond.pnTrue ? 0 : 1;
			int falseIndex = 1 - trueIndex;
			Const trueNode = (Const) phi.getPred(trueIndex);
			Const falseNode = (Const) phi.getPred(falseIndex);

			// setxx has a hardcoded meaning and true is 1!
			if (!trueNode.getTarval().isOne()) {
				match.cmp().setRelation(Util.invert(match.cmp().getRelation()));
				Const tmp = trueNode;
				trueNode = falseNode;
				falseNode = tmp;
			}

			Node mux = match.cmp().getGraph().newMux(match.cmp().getBlock(), match.cmp(), falseNode, trueNode);

			for (BackEdges.Edge out : BackEdges.getOuts(phi)) {
				out.node.setPred(out.pos, mux);
			}

			Node jmp = match.cmp().getGraph().newJmp(match.cmp().getBlock());
			binding_irnode.set_irn_in(match.afterBlock().ptr, 1, Node.getBufferFromNodeList(new Node[]{jmp}));

			changed = true;
		}

		if (changed) {
			for (BackEdges.Edge edge : BackEdges.getOuts(match.afterBlock())) {
				if (edge.node instanceof Phi phi && phi.getMode().equals(Mode.getM())) {
					for (BackEdges.Edge out : BackEdges.getOuts(phi)) {
						out.node.setPred(out.pos, phi.getPred(0));
					}
					Node[] newPreds =
						Util.predsStream(graph.getEnd()).filter(it -> !it.equals(phi)).toArray(Node[]::new);
					binding_irnode.set_irn_in(graph.getEnd().ptr, newPreds.length,
						Node.getBufferFromNodeList(newPreds));
					Graph.killNode(phi);
				}
			}
		}

		return changed;
	}

	private static List<Phi> findPhi(CondToBoolShortcutPattern.Match match) {
		List<Phi> foundPhis = new ArrayList<>();
		for (BackEdges.Edge edge : BackEdges.getOuts(match.afterBlock())) {
			if (edge.node instanceof Phi phi && !phi.getMode().equals(Mode.getM())) {
				if (!(phi.getPred(0) instanceof Const left) || !(phi.getPred(1) instanceof Const right)) {
					return List.of();
				}
				if (left.getTarval().isOne() && right.getTarval().isNull()) {
					foundPhis.add(phi);
				}
				if (left.getTarval().isNull() && right.getTarval().isOne()) {
					foundPhis.add(phi);
				}
			}
		}

		return foundPhis;
	}

	private void applyBooleanOptimization() {
		// We want to walk the normal walk. Starting at the end and walking up the pred chain allows us to
		// match larger patterns first. This is relevant as e.g. associativeMul should simplify multiplication before
		// the strength reduction converts only *parts* of it to shifts
		graph.walk(this);
	}

	@Override
	public void defaultVisit(Node node) {
		hasChanged |= OPTIMIZATIONS.optimize(node, node.getGraph(), node.getBlock());
	}

	@FiAscii("""
		┌──────────┐
		│ input: * │
		└──┬───┬───┘
		   │   │
		   │   │
		┌──▼───▼───┐
		│ cmp: Cmp │
		└──────────┘""")
	public static Optional<CmpWithSameInputsPattern.Match> cmpWithSameInputs(Node node) {
		return CmpWithSameInputsPattern.match(node);
	}

	@FiAscii("""
		         ┌──────────┐
		         │ cmp: Cmp │
		         └────┬─────┘
		              │
		        ┌─────▼──────┐
		        │ cond: Cond │
		        └──┬─────┬───┘
		           │     │
		┌──────────▼─┐ ┌─▼─────────┐
		│ out1: Proj │ │out2: Proj │
		└──────┬─────┘ └────┬──────┘
		       │            │
		       │            │
		       │            │
		    ┌──▼────────────▼──┐
		    │afterBlock: Block │
		    └──────────────────┘""")
	public static Optional<CondToBoolShortcutPattern.Match> condToBoolShortcut(Node node) {
		return CondToBoolShortcutPattern.match(node);
	}
}
