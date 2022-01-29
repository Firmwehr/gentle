package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.fiascii.FiAscii;
import com.github.firmwehr.fiascii.generated.CmpWithSameInputsPattern;
import com.github.firmwehr.gentle.output.Logger;
import firm.BackEdges;
import firm.Graph;
import firm.bindings.binding_irgopt;
import firm.nodes.Const;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;

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

			Node newInput = graph.newConst(match.cmp().getMode().getNull());
			match.cmp().setLeft(newInput);
			match.cmp().setRight(newInput);
			return true;
		}) //
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
}
