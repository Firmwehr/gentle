package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.fiascii.FiAscii;
import com.github.firmwehr.fiascii.generated.AddZeroPattern;
import com.github.firmwehr.fiascii.generated.SubtractFromZeroPattern;
import com.github.firmwehr.gentle.output.Logger;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.TargetValue;
import firm.bindings.binding_irgopt;
import firm.nodes.Add;
import firm.nodes.Const;
import firm.nodes.Div;
import firm.nodes.Mul;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Sub;

import java.util.Optional;

import static com.github.firmwehr.gentle.util.GraphDumper.dumpGraph;

public class ArithmeticOptimization extends NodeVisitor.Default {

	private static final Logger LOGGER = new Logger(ArithmeticOptimization.class);

	private boolean hasChanged;
	private final Graph graph;

	public ArithmeticOptimization(Graph graph) {
		this.graph = graph;
	}

	public static GraphOptimizationStep arithmeticOptimization() {
		return GraphOptimizationStep.builder()
			.withDescription("ArithmeticOptimization")
			.withOptimizationFunction(graph -> {
				int runs = 0;
				while (true) {
					// Needs to be done in each iteration apparently?
					BackEdges.enable(graph);

					ArithmeticOptimization arithmeticOptimization = new ArithmeticOptimization(graph);
					arithmeticOptimization.applyArithmeticOptimization();
					binding_irgopt.remove_bads(graph.ptr);
					binding_irgopt.remove_unreachable_code(graph.ptr);

					// testing has shown that back edges get disabled anyway for some reason, but we don't like
					// problems
					BackEdges.disable(graph);

					if (!arithmeticOptimization.hasChanged) {
						break;
					} else if (LOGGER.isDebugEnabled()) {
						dumpGraph(graph, "arithmetic-iteration");
					}
					runs++;
				}
				dumpGraph(graph, "arithmetic");
				return runs > 0;
			})
			.build();
	}

	private void applyArithmeticOptimization() {
		graph.walkTopological(this);
	}

	@Override
	public void visit(Add node) {
		addWithZero(node).ifPresent(match -> exchange(match.add(), match.any()));
	}

	@Override
	public void visit(Sub node) {
		TargetValue leftVal = tarValOf(node.getLeft());
		TargetValue rightVal = tarValOf(node.getRight());

		subtractFromZero(node).ifPresent(
			match -> exchange(match.sub(), node.getGraph().newMinus(node.getBlock(), match.rhs())));

		if (rightVal.isNull()) {
			exchange(node, node.getLeft());
			return;
		}
		//		if (leftVal.isNull()) {
		//			exchange(node, node.getGraph().newMinus(node.getBlock(), node.getRight()));
		//			return;
		//		}
		if (leftVal.isConstant() && leftVal.equals(rightVal)) {
			exchange(node, node.getGraph().newConst(0, node.getLeft().getMode()));
		}
	}

	@Override
	public void visit(Mul node) {
		TargetValue leftVal = tarValOf(node.getLeft());
		TargetValue rightVal = tarValOf(node.getRight());

		// some algebraic identities
		// 1 * a => a
		if (leftVal.isOne()) {
			exchange(node, node.getRight());
			return;
		}
		// a * 1 => a
		if (rightVal.isOne()) {
			exchange(node, node.getLeft());
			return;
		}
		// 0 * a => 0 && a * 0 => 0
		if (leftVal.isNull() || rightVal.isNull()) {
			exchange(node, node.getGraph().newConst(0, node.getLeft().getMode()));
			return;
		}
		// a * -1 => -a
		if (rightVal.isConstant() && rightVal.isNegative() && rightVal.abs().isOne()) {
			exchange(node, node.getGraph().newMinus(node.getBlock(), node.getLeft()));
			return;
		}
		// -1 * a => -a
		if (leftVal.isConstant() && leftVal.isNegative() && leftVal.abs().isOne()) {
			exchange(node, node.getGraph().newMinus(node.getBlock(), node.getRight()));
		}
		// TODO (maybe): 2 * a => a << 2
	}

	@Override
	public void visit(Div node) {
		TargetValue rightVal = tarValOf(node.getRight());
		// a / 1 => a
		if (rightVal.isOne()) {
			replace(node, node.getMem(), node.getLeft());
			return;
		}
		// a / -1 => -a
		if (rightVal.isConstant() && rightVal.abs().isOne() && rightVal.isNegative()) {
			replace(node, node.getMem(), node.getGraph().newMinus(node.getBlock(), node.getLeft()));
		}
	}

	private void exchange(Node victim, Node murderer) {
		LOGGER.debug("Exchanging %-25s with %-25s", victim, murderer);
		Node selectedReplacement = murderer;
		if (!victim.getMode().equals(murderer.getMode())) {
			if (murderer instanceof Const constant) {
				selectedReplacement = victim.getGraph().newConst(constant.getTarval().convertTo(victim.getMode()));
				LOGGER.debug("Changed    %-25s to %-25s", murderer, selectedReplacement);
			} else {
				selectedReplacement = victim.getGraph().newConv(victim.getBlock(), murderer, victim.getMode());
				LOGGER.debug("Introduced conversion node %-25s to mode %s", selectedReplacement, victim.getMode());
			}
		}
		Graph.exchange(victim, selectedReplacement);
		hasChanged = true;
	}

	/**
	 * <pre>
	 * 	  Div
	 * 	 /   \
	 * 	M    Res
	 * </pre>
	 * Mod and Div have side effects on memory, we can't just replace them like everything else. Instead, we need to
	 * rewire their memory and output projections.
	 *
	 * @param node the div/mod node to replace
	 * @param previousMemory the memory input of the node
	 * @param replacement the replacement (maybe constant) node
	 */
	private void replace(Node node, Node previousMemory, Node replacement) {
		for (BackEdges.Edge out : BackEdges.getOuts(node)) {
			if (out.node.getMode().equals(Mode.getM())) {
				exchange(out.node, previousMemory);
			} else {
				exchange(out.node, replacement);
			}
		}
	}

	private TargetValue tarValOf(Node node) {
		if (node instanceof Const constant) {
			return constant.getTarval();
		}

		return TargetValue.getBad();
	}

	@FiAscii("""
		┌──────────────┐      ┌──────┐
		│zero: Const 0 │      │any: *│
		└────────┬─────┘      └───┬──┘
		         │                │
		         └────┬───────────┘
		              │
		         ┌────▼─────┐
		         │ add: Add │
		         └──────────┘""")
	public static Optional<AddZeroPattern.Match> addWithZero(Node node) {
		return AddZeroPattern.match(node);
	}

	@FiAscii("""
		┌──────────────┐   ┌──────┐
		│minus: Const 0│   │rhs: *│
		└──────────┬───┘   └──┬───┘
		           │          │
		           │   ┌──────┘
		           │   │
		         ┌─▼───▼──┐
		         │sub: Sub│
		         └────────┘""")
	public static Optional<SubtractFromZeroPattern.Match> subtractFromZero(Node node) {
		return SubtractFromZeroPattern.match(node);
	}
}
