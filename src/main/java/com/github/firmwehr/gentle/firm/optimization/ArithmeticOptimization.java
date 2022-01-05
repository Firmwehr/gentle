package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.fiascii.FiAscii;
import com.github.firmwehr.fiascii.generated.AddMinusPattern;
import com.github.firmwehr.fiascii.generated.AddZeroPattern;
import com.github.firmwehr.fiascii.generated.AssociativeAddPattern;
import com.github.firmwehr.fiascii.generated.AssociativeMulPattern;
import com.github.firmwehr.fiascii.generated.DistributivePattern;
import com.github.firmwehr.fiascii.generated.DivByNegOnePattern;
import com.github.firmwehr.fiascii.generated.DivByOnePattern;
import com.github.firmwehr.fiascii.generated.SubtractFromZeroPattern;
import com.github.firmwehr.fiascii.generated.SubtractSamePattern;
import com.github.firmwehr.fiascii.generated.SubtractZeroPattern;
import com.github.firmwehr.fiascii.generated.TimesNegOnePattern;
import com.github.firmwehr.fiascii.generated.TimesOnePattern;
import com.github.firmwehr.fiascii.generated.TimesZeroPattern;
import com.github.firmwehr.gentle.firm.Util;
import com.github.firmwehr.gentle.output.Logger;
import firm.BackEdges;
import firm.Graph;
import firm.bindings.binding_irgopt;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;

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
	public void defaultVisit(Node node) {
		Graph graph = node.getGraph();
		Node block = node.getBlock();

		// TODO (maybe): 2 * a => a << 2
		timesOne(node).ifPresent(match -> exchange(match.mul(), match.other()));
		timesZero(node).ifPresent(match -> exchange(match.mul(), graph.newConst(0, match.mul().getMode())));
		timesNegOne(node).ifPresent(match -> exchange(match.mul(), graph.newMinus(block, match.other())));

		divByNegOne(node).ifPresent(
			match -> replace(match.div(), match.div().getMem(), graph.newMinus(block, match.other())));
		divByOne(node).ifPresent(match -> replace(match.div(), match.mem(), match.other()));

		addWithZero(node).ifPresent(match -> exchange(match.add(), match.any()));

		subtractSame(node).ifPresent(match -> exchange(match.sub(), graph.newConst(0, match.sub().getMode())));
		subtractFromZero(node).ifPresent(match -> exchange(match.sub(), graph.newMinus(block, match.rhs())));
		subtractZero(node).ifPresent(match -> exchange(match.sub(), match.lhs()));

		addMinus(node).ifPresent(match -> exchange(match.add(), graph.newSub(block, match.other(), match.value())));

		associativeAdd(node).ifPresent(match -> {
			// we don't care about left/right here, just set both again *somewhere*
			match.inner().setLeft(match.a());
			match.inner().setRight(match.b());
			// same for outer >= vs =>
			match.outer().setLeft(match.inner());
			match.outer().setRight(match.c());
			hasChanged = true;
		});

		associativeMul(node).ifPresent(match -> {
			// we don't care about left/right here, just set both again *somewhere*
			match.inner().setLeft(match.a());
			match.inner().setRight(match.b());
			// same for outer >= vs =>
			match.outer().setLeft(match.inner());
			match.outer().setRight(match.c());
			hasChanged = true;
		});

		distributive(node).ifPresent(match -> {
			System.out.println(match);
			exchange(match.add(), graph.newMul(block, match.factor(), graph.newAdd(block, match.av(), match.bv())));
		});
	}

	private void exchange(Node victim, Node murderer) {
		Util.exchange(victim, murderer);
		hasChanged = true;
	}

	private void replace(Node node, Node previousMemory, Node replacement) {
		Util.replace(node, previousMemory, replacement);
		hasChanged = true;
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

	@FiAscii("""
		┌──────┐   ┌──────────────┐
		│lhs: *│   │minus: Const 0│
		└───┬──┘   └┬─────────────┘
		    │       │
		    └────┐  │
		         │  │
		      ┌──▼──▼──┐
		      │sub: Sub│
		      └────────┘""")
	public static Optional<SubtractZeroPattern.Match> subtractZero(Node node) {
		return SubtractZeroPattern.match(node);
	}

	@FiAscii("""
		 ┌──────┐
		 │val: *│
		 └─┬──┬─┘
		   │  │
		   │  │
		┌──▼──▼──┐
		│sub: Sub│
		└────────┘""")
	public static Optional<SubtractSamePattern.Match> subtractSame(Node node) {
		return SubtractSamePattern.match(node);
	}

	@FiAscii("""
		┌────────────┐  ┌─────────┐
		│one: Const 1│  │other: * │
		└─────┬──────┘  └────┬────┘
		      │              │
		      └──────┬───────┘
		             │
		         ┌───▼────┐
		         │mul: Mul│
		         └────────┘""")
	public static Optional<TimesOnePattern.Match> timesOne(Node node) {
		return TimesOnePattern.match(node);
	}

	@FiAscii("""
		┌─────────────┐  ┌─────────┐
		│one: Const -1│  │other: * │
		└─────┬───────┘  └────┬────┘
		      │               │
		      └──────┬────────┘
		             │
		         ┌───▼────┐
		         │mul: Mul│
		         └────────┘""")
	public static Optional<TimesNegOnePattern.Match> timesNegOne(Node node) {
		return TimesNegOnePattern.match(node);
	}

	@FiAscii("""
		┌────────────┐  ┌─────────┐
		│one: Const 0│  │other: * │
		└─────┬──────┘  └────┬────┘
		      │              │
		      └──────┬───────┘
		             │
		         ┌───▼────┐
		         │mul: Mul│
		         └────────┘""")
	public static Optional<TimesZeroPattern.Match> timesZero(Node node) {
		return TimesZeroPattern.match(node);
	}

	@FiAscii("""
		┌─────────────────┐ ┌─────────┐
		│mem: * ; +memory │ │other: * │
		└─────────────┬───┘ └┬────────┘
		              │      │  ┌────────────┐
		              │  ┌───┘  │one: Const 1│
		              │  │      └─┬──────────┘
		              │  │  ┌─────┘
		              │  │  │
		             ┌▼──▼──▼─┐
		             │div: Div│
		             └────────┘""")
	public static Optional<DivByOnePattern.Match> divByOne(Node node) {
		return DivByOnePattern.match(node);
	}

	@FiAscii("""
		┌─────────────────┐ ┌─────────┐
		│mem: * ; +memory │ │other: * │
		└─────────────┬───┘ └┬────────┘
		              │      │  ┌─────────────┐
		              │  ┌───┘  │one: Const -1│
		              │  │      └─┬───────────┘
		              │  │  ┌─────┘
		              │  │  │
		             ┌▼──▼──▼─┐
		             │div: Div│
		             └────────┘""")
	public static Optional<DivByNegOnePattern.Match> divByNegOne(Node node) {
		return DivByNegOnePattern.match(node);
	}

	@FiAscii("""
		┌──────────┐
		│ value: * │
		└──────┬───┘
		       │
		       │
		┌──────▼───────┐   ┌──────────┐
		│ minus: Minus │   │ other: * │
		└───────┬──────┘   └────┬─────┘
		        │               │
		        └───────┬───────┘
		                │
		           ┌────▼─────┐
		           │ add: Add │
		           └──────────┘""")
	public static Optional<AddMinusPattern.Match> addMinus(Node node) {
		return AddMinusPattern.match(node);
	}

	@FiAscii("""
		             ┌──────────┐ ┌──────┐
		             │ b: Const │ │ c: * │
		             └─────┬────┘ └───┬──┘
		                   │          │
		                   └─────┬────┘
		                         │
		    ┌──────────┐  ┌──────▼─────┐
		    │ a: Const │  │ inner: Add │
		    └──────┬───┘  └───┬────────┘
		           │          │
		           └───┬──────┘
		               │
		         ┌─────▼──────┐
		         │ outer: Add │
		         └────────────┘
		""")
	public static Optional<AssociativeAddPattern.Match> associativeAdd(Node node) {
		return AssociativeAddPattern.match(node);
	}

	@FiAscii("""
		             ┌──────────┐ ┌──────┐
		             │ b: Const │ │ c: * │
		             └─────┬────┘ └───┬──┘
		                   │          │
		                   └─────┬────┘
		                         │
		    ┌──────────┐  ┌──────▼─────┐
		    │ a: Const │  │ inner: Mul │
		    └──────┬───┘  └───┬────────┘
		           │          │
		           └───┬──────┘
		               │
		         ┌─────▼──────┐
		         │ outer: Mul │
		         └────────────┘
		""")
	public static Optional<AssociativeMulPattern.Match> associativeMul(Node node) {
		return AssociativeMulPattern.match(node);
	}

	@FiAscii("""
		┌───────┐     ┌───────────────┐   ┌───────┐
		│ av: * │     │ factor: *     │   │ bv: * │
		└────┬──┘     └───┬───────┬───┘   └───┬───┘
		     │            │       │           │
		     └──────┬─────┘       └───┬───────┘
		            │                 │
		        ┌───▼────┐        ┌───▼────┐
		        │ a: Mul │        │ b: Mul │
		        └────┬───┘        └─────┬──┘
		             │                  │
		             └────────┬─────────┘
		                      │
		                 ┌────▼────┐
		                 │ add: Add│
		                 └─────────┘""")
	public static Optional<DistributivePattern.Match> distributive(Node node) {
		return DistributivePattern.match(node);
	}
}
