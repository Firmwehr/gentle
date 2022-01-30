package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.fiascii.FiAscii;
import com.github.firmwehr.fiascii.generated.AddMinusPattern;
import com.github.firmwehr.fiascii.generated.AddSamePattern;
import com.github.firmwehr.fiascii.generated.AddZeroPattern;
import com.github.firmwehr.fiascii.generated.AssociativeAddPattern;
import com.github.firmwehr.fiascii.generated.AssociativeMulPattern;
import com.github.firmwehr.fiascii.generated.DistributivePattern;
import com.github.firmwehr.fiascii.generated.DistributiveShiftsPattern;
import com.github.firmwehr.fiascii.generated.DivByConstPattern;
import com.github.firmwehr.fiascii.generated.DivByNegOnePattern;
import com.github.firmwehr.fiascii.generated.DivByOnePattern;
import com.github.firmwehr.fiascii.generated.DoubleShiftLeftPattern;
import com.github.firmwehr.fiascii.generated.MinusMinusPattern;
import com.github.firmwehr.fiascii.generated.MixedDistributivePattern;
import com.github.firmwehr.fiascii.generated.ModByConstPattern;
import com.github.firmwehr.fiascii.generated.MulInAddPattern;
import com.github.firmwehr.fiascii.generated.MulInShiftLeftPattern;
import com.github.firmwehr.fiascii.generated.ShiftRightLeftPattern;
import com.github.firmwehr.fiascii.generated.ShiftRightSignedLeftPattern;
import com.github.firmwehr.fiascii.generated.SubtractFromZeroPattern;
import com.github.firmwehr.fiascii.generated.SubtractSamePattern;
import com.github.firmwehr.fiascii.generated.SubtractZeroPattern;
import com.github.firmwehr.fiascii.generated.TimesConstPattern;
import com.github.firmwehr.fiascii.generated.TimesNegOnePattern;
import com.github.firmwehr.fiascii.generated.TimesOnePattern;
import com.github.firmwehr.fiascii.generated.TimesZeroPattern;
import com.github.firmwehr.gentle.firm.Util;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.Maths;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.TargetValue;
import firm.bindings.binding_irgopt;
import firm.nodes.Const;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Shl;

import java.util.Optional;

import static com.github.firmwehr.gentle.util.GraphDumper.dumpGraph;

public class ArithmeticOptimization extends NodeVisitor.Default {

	private static final Logger LOGGER = new Logger(ArithmeticOptimization.class);

	private static final OptimizationList OPTIMIZATIONS = OptimizationList.builder()
		.addStep(ArithmeticOptimization::timesZero,
			(match, graph, block) -> exchange(match.mul(), newConst(graph, 0, match.mul().getMode())))
		.addStep(ArithmeticOptimization::timesNegOne,
			(match, graph, block) -> exchange(match.mul(), graph.newMinus(block, match.other())))
		.addStep(ArithmeticOptimization::divByNegOne,
			(match, graph, block) -> replace(match.div(), match.div().getMem(), graph.newMinus(block, match.other())))
		.addStep(ArithmeticOptimization::divByOne,
			(match, graph, block) -> replace(match.div(), match.mem(), match.other()))
		.addStep(ArithmeticOptimization::addWithZero, (match, graph, block) -> exchange(match.add(), match.any()))
		.addStep(ArithmeticOptimization::addSame, (match, graph, block) -> exchange(match.add(),
			graph.newMul(block, match.val(), newConst(graph, 2, match.add().getMode()))))
		.addStep(ArithmeticOptimization::subtractSame,
			(match, graph, block) -> exchange(match.sub(), graph.newConst(0, match.sub().getMode())))
		.addStep(ArithmeticOptimization::subtractFromZero,
			(match, graph, block) -> exchange(match.sub(), graph.newMinus(block, match.rhs())))
		.addStep(ArithmeticOptimization::subtractZero, (match, graph, block) -> exchange(match.sub(), match.lhs()))
		.addStep(ArithmeticOptimization::addMinus,
			(match, graph, block) -> exchange(match.add(), graph.newSub(block, match.other(), match.value())))
		.addStep(ArithmeticOptimization::associativeAdd, (match, graph, block) -> {
			// we don't care about left/right here, just set both again *somewhere*
			Node newInner = graph.newAdd(block, match.a(), match.b());
			return exchange(match.outer(), graph.newAdd(block, newInner, match.c()));
		})
		.addStep(ArithmeticOptimization::associativeMul, (match, graph, block) -> {
			// we don't care about left/right here, just set both again *somewhere*
			Node newInner = graph.newMul(block, match.a(), match.b());
			return exchange(match.outer(), graph.newMul(block, newInner, match.c()));
		})
		.addStep(ArithmeticOptimization::distributive, (match, graph, block) -> exchange(match.add(),
			graph.newMul(block, match.factor(), graph.newAdd(block, match.av(), match.bv()))))
		.addStep(ArithmeticOptimization::distributiveShifts, ArithmeticOptimization::collapseDistributiveShifts)
		.addStep(ArithmeticOptimization::mixedDistributive, ArithmeticOptimization::collapseMixedDistributive)
		.addStep(ArithmeticOptimization::mulInAdd, (match, graph, block) -> exchange(match.outer(),
			graph.newMul(block, match.factor(),
				graph.newAdd(block, newConst(graph, 1, match.outer().getMode()), match.bv()))))
		.addStep(ArithmeticOptimization::timesOne, (match, graph, block) -> exchange(match.mul(), match.other()))
		.addStep( //
			ArithmeticOptimization::divByConst, //
			(match, graph, block) -> constructFastDiv(match, graph) //
				.map(replacement -> replace(match.div(), match.mem(), replacement)) //
				.orElse(false) //
		)
		.addStep( //
			ArithmeticOptimization::modByConst, //
			(match, graph, block) -> constructFastMod(match, graph) //
				.map(replacement -> replace(match.mod(), match.mem(), replacement)) //
				.orElse(false) //
		)
		.addStep(ArithmeticOptimization::timesConst, ArithmeticOptimization::acceptTimesConst)
		.addStep(ArithmeticOptimization::minusMinus, (match, graph, block) -> exchange(match.repl(), match.value()))
		.addStep(ArithmeticOptimization::shiftRightSignedLeft,
			(m, graph, block) -> shiftRightLeft(graph, block, m.left(), m.lVal(), m.rVal(), m.value()))
		.addStep(ArithmeticOptimization::shiftRightLeft,
			(m, graph, block) -> shiftRightLeft(graph, block, m.left(), m.lVal(), m.rVal(), m.value()))
		.addStep(ArithmeticOptimization::mulInShiftLeft, (match, graph, block) -> exchange(match.outer(),
			graph.newMul(block, match.value(), graph.newShl(block, match.ci(), match.co()))))
		.addStep(ArithmeticOptimization::doubleShiftLeft, ArithmeticOptimization::collapseDoubleShiftLeft)
		.build();

	private boolean hasChanged;
	private final Graph graph;

	public ArithmeticOptimization(Graph graph) {
		this.graph = graph;
	}

	public static GraphOptimizationStep<Graph, Boolean> arithmeticOptimization() {
		return GraphOptimizationStep.<Graph, Boolean>builder()
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
				boolean changed = runs > 0;
				if (changed) {
					dumpGraph(graph, "arithmetic");
				}
				return changed;
			})
			.build();
	}

	private static boolean collapseDistributiveShifts(DistributiveShiftsPattern.Match match, Graph graph, Node block) {
		int a = match.av().getTarval().asInt();
		int b = match.bv().getTarval().asInt();
		Mode mode = match.add().getMode();
		if (a == b) { // (n << k) + (n << k) == (n << (k + 1))
			int newShiftValue = a + 1;
			if (newShiftValue >= mode.getSizeBits()) {
				return exchange(match.add(), newConst(graph, 0, mode));
			}
			Node newShiftConstant = newConst(graph, newShiftValue, Mode.getIu());
			return exchange(match.add(), graph.newShl(block, match.factor(), newShiftConstant));
		}
		return exchange(match.add(), graph.newMul(block, match.factor(), newConst(graph, (1 << a) + (1 << b), mode)));
	}

	private static boolean collapseMixedDistributive(MixedDistributivePattern.Match match, Graph graph, Node block) {
		int b = 1 << match.bv().getTarval().asInt();
		Node newAdd = graph.newAdd(block, match.av(), newConst(graph, b, match.add().getMode()));
		return exchange(match.add(), graph.newMul(block, match.factor(), newAdd));
	}

	private void applyArithmeticOptimization() {
		// We want to walk the normal walk. Starting at the end and walking up the pred chain allows us to
		// match larger patterns first. This is relevant as e.g. associativeMul should simplify multiplication before
		// the strength reduction converts only *parts* of it to shifts
		graph.walk(this);
	}

	@Override
	public void defaultVisit(Node node) {
		hasChanged |= OPTIMIZATIONS.optimize(node, node.getGraph(), node.getBlock());
	}

	private static boolean collapseDoubleShiftLeft(DoubleShiftLeftPattern.Match match, Graph graph, Node block) {
		int shiftsAdded = match.ci().getTarval().add(match.co().getTarval()).asInt();
		if (shiftsAdded >= match.outer().getMode().getSizeBits()) {
			return exchange(match.outer(), newConst(graph, 0, match.outer().getMode()));
		}
		return exchange(match.outer(), graph.newShl(block, match.value(), newConst(graph, shiftsAdded, Mode.getIu())));
	}

	private static Optional<Node> constructFastMod(ModByConstPattern.Match match, Graph graph) {
		int divisor = match.value().getTarval().asInt();
		int absDivisor = Math.abs(divisor);
		if (absDivisor == 1) { // x % ± 1 is always zero
			return Optional.of(newConst(graph, 0, Mode.getIs()));
		}
		Node block = match.mod().getBlock();
		Optional<Node> replacementOpt = constructFastModDivCommon(absDivisor, block, match.other(), graph);
		if (replacementOpt.isEmpty()) {
			return replacementOpt;
		}
		Node replacement = graph.newMul(block, replacementOpt.get(), newConst(graph, absDivisor, Mode.getIs()));
		replacement = graph.newSub(block, match.other(), replacement);
		return Optional.of(replacement);
	}

	private static Optional<Node> constructFastDiv(DivByConstPattern.Match match, Graph graph) {
		int divisor = match.value().getTarval().asInt();
		Node block = match.div().getBlock();
		return constructFastModDivCommon(divisor, block, match.other(), graph);
	}

	private static Optional<Node> constructFastModDivCommon(int divisor, Node block, Node dividend, Graph graph) {
		boolean isNegative = divisor < 0;
		int absDivisor = Math.abs(divisor);
		int k = Maths.floorLog2(absDivisor);
		if (1 << k == absDivisor) { // true for 2**k and Integer.MIN_VALUE, following works for all of them
			// Hacker's Delight, 2nd Edition, Chapter 10-1
			Node quotient = dividend;
			if (k != 1) { // 1 - 1 == 0; n >>> 0 == n
				quotient = graph.newShrs(block, quotient, newConst(graph, k - 1, Mode.getIu()));  // shrsi t,n,k-1
			}
			quotient = graph.newShr(block, quotient, newConst(graph, 32 - k, Mode.getIu()));      // shri  t,t,32-k
			quotient = graph.newAdd(block, dividend, quotient);                                   // add   t,n,t
			quotient = graph.newShrs(block, quotient, newConst(graph, k, Mode.getIu()));          // shrsi q,t,k
			if (isNegative) {
				// as the part above calculates other / 2**k, we need to negate the result
				quotient = graph.newMinus(block, quotient);
			}
			return Optional.of(quotient);
		} else if (absDivisor >= 2) {
			// end boss, be careful
			// Hacker's Delight, 2nd Edition, Chapter 10-4 - 10-6
			// This implementation is very similar to the one found in the OpenJDK
			// to avoid confusion about control flow
			// https://github.com/openjdk/jdk/blob/35172cdaf38d83cd3ed57a5436bf985dde2d802b/src/hotspot/share/opto/divnode.cpp#L160
			int N = 32;
			// we always calculate the magic number for the absolute divisor
			// as the case of -2**k is handled by the last Sub
			MagicDiv magicDiv = magic(absDivisor);
			// We could use Mulh nodes, but that requires us to have codegen for it too
			// - and as x86 has no specific instruction, it's not really worth to go that way
			Node magic = newConst(graph, magicDiv.magicConst(),
				Mode.getLs());         // we want the upper 32 bits of a 64 bit mul
			Node dividendLong =
				graph.newConv(block, dividend, Mode.getLs());   // so we need to convert the arguments first
			Node mulHi = graph.newMul(block, dividendLong, magic);
			if (magicDiv.magicConst() < 0) {
				mulHi = graph.newShrs(block, mulHi, newConst(graph, N, Mode.getIu()));
				mulHi = graph.newConv(block, mulHi, Mode.getIs());
				mulHi = graph.newAdd(block, dividend, mulHi);
				if (magicDiv.shiftConst() != 0) {
					mulHi = graph.newShrs(block, mulHi, newConst(graph, magicDiv.shiftConst(), Mode.getIu()));
				}
			} else {
				// we can combine the 32 bit shift with the magic shift for 64 bit operations
				mulHi = graph.newShrs(block, mulHi, newConst(graph, N + magicDiv.shiftConst(), Mode.getIu()));
				mulHi = graph.newConv(block, mulHi, Mode.getIs());
			}

			Node addend0 = mulHi;
			Node addend1 = graph.newShrs(block, dividend, newConst(graph, -1, Mode.getIu()));
			if (isNegative) {
				Node tmp = addend0;
				addend0 = addend1;
				addend1 = tmp;
			}

			return Optional.of(graph.newSub(block, addend0, addend1));
		} else {
			return Optional.empty(); // e.g. div by zero, we just don't care about that
		}
	}

	// Hacker's Delight, 2nd Edition, Figure 10-1, rewritten in Java
	// using long for "unsigned" ints
	private static MagicDiv magic(int d) {
		long two31 = 0x80000000L;
		int ad = Math.abs(d);
		long t = two31 + (d >>> 31);
		long anc = t - 1 - t % ad;
		long q1 = two31 / anc;
		long r1 = two31 - q1 * anc;
		long q2 = two31 / ad;
		long r2 = two31 - q2 * ad;
		int p = 31;
		long delta;
		do {
			p++;
			q1 *= 2;
			r1 *= 2;
			if (r1 >= anc) {
				q1++;
				r1 -= anc;
			}
			q2 *= 2;
			r2 *= 2;
			if (r2 >= ad) {
				q2++;
				r2 -= ad;
			}
			delta = ad - r2;
		} while (q1 < delta || (q1 == delta && r1 == 0));
		long M = q2 + 1;
		if (d < 0) {
			M = -M;
		}
		return new MagicDiv((int) M, p - 32);
	}

	private record MagicDiv(
		int magicConst,
		int shiftConst
	) {

	}

	private static boolean acceptTimesConst(TimesConstPattern.Match match, Graph graph, Node block) {
		int absVal = Math.abs(match.constVal().getTarval().asInt());
		int log2 = Maths.floorLog2(absVal);
		if ((1 << log2) != absVal) {
			return false;
		}
		Node replaced = graph.newShl(block, match.other(), newConst(graph, log2, Mode.getIu()));
		if (match.constVal().getTarval().isNegative()) {
			replaced = graph.newMinus(block, replaced);
		}
		return exchange(match.mul(), replaced);
	}

	private static boolean shiftRightLeft(
		Graph graph, Node block, Shl left, Const leftValue, Const rightValue, Node value
	) {
		int leftShift = leftValue.getTarval().asInt();
		int rightShift = rightValue.getTarval().asInt();
		int diff = rightShift - leftShift;
		if (diff == 0) {
			// only n trailing bits are set to 0, nothing else changes
			int upperBitsMask = -(1 << rightShift);
			return exchange(left, graph.newAnd(block, value, newConst(graph, upperBitsMask, Mode.getIs())));
		}
		// we could partly shift if diff is nonzero, but it's rather not worth it?
		return false;
	}

	private static Node newConst(Graph graph, int i, Mode mode) {
		return graph.newConst(new TargetValue(i, mode));
	}

	private static boolean exchange(Node victim, Node murderer) {
		Util.exchange(victim, murderer);
		return true;
	}

	private static boolean replace(Node node, Node previousMemory, Node replacement) {
		Util.replace(node, previousMemory, replacement);
		return true;
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
		 ┌──────┐
		 │val: *│
		 └─┬──┬─┘
		   │  │
		   │  │
		┌──▼──▼──┐
		│add: Add│
		└────────┘""")
	public static Optional<AddSamePattern.Match> addSame(Node node) {
		return AddSamePattern.match(node);
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
		┌───────┐     ┌───────────────┐   ┌───────┐ Memory side effects from av and/or bv are kept
		│ av: * │     │ factor: *     │   │ bv: * │ in order by memory projections, so we don't
		└────┬──┘     └───┬───────┬───┘   └───┬───┘ need to care about that.
		     │            │       │           │     Memory side effects from factor can be either
		     └──────┬─────┘       └───┬───────┘     ignored (e.g. if av and bv are constants), or
		            │                 │             the memory projections keep it in the right order.
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


	@FiAscii("""
		      ┌───────────┐    ┌───────┐
		      │ factor: * │    │ bv: * │ Similar to DistributivePattern, with av = 1
		      └────┬─────┬┘    └───┬───┘
		           │     │         │
		           │     └───────┬─┘
		           │             │
		           │      ┌──────▼─────┐
		           │      │ inner: Mul │
		           │      └───┬────────┘
		           │          │
		           └───┬──────┘
		               │
		         ┌─────▼──────┐
		         │ outer: Add │
		         └────────────┘
		""")
	public static Optional<MulInAddPattern.Match> mulInAdd(Node node) {
		return MulInAddPattern.match(node);
	}

	@FiAscii("""
		           ┌───────────┐
		           │ factor: * │
		           └─┬───────┬─┘
		             │       │
		    ┌────────┘       └───┐
		    │                    │
		    │   ┌────────────┐   │     ┌───────────┐
		    │   │  av: Const │   │     │ bv: Const │
		    │   └──┬─────────┘   │     └┬──────────┘
		    │      │             │      │
		┌───▼──────▼┐          ┌─▼──────▼───┐
		│ left: Shl │          │ right: Shl │
		└────────┬──┘          └─┬──────────┘
		         │               │
		         └───────┬───────┘
		                 │
		                 │
		           ┌─────▼────┐
		           │ add: Add │
		           └──────────┘""")
	public static Optional<DistributiveShiftsPattern.Match> distributiveShifts(Node node) {
		return DistributiveShiftsPattern.match(node);
	}

	@FiAscii("""
		┌───────┐     ┌───────────────┐   ┌───────────┐
		│ av: * │     │ factor: *     │   │ bv: Const │ Similar to distributive
		└────┬──┘     └───┬───────┬───┘   └───┬───────┘ but one side has a Shl
		     │            │       │           │
		     └──────┬─────┘       └───┐  ┌────┘
		            │                 │  │
		        ┌───▼────┐        ┌───▼──▼─┐
		        │ a: Mul │        │ b: Shl │
		        └────┬───┘        └─────┬──┘
		             │                  │
		             └────────┬─────────┘
		                      │
		                 ┌────▼─────┐
		                 │ add: Add │
		                 └──────────┘""")
	public static Optional<MixedDistributivePattern.Match> mixedDistributive(Node node) {
		return MixedDistributivePattern.match(node);
	}

	@FiAscii("""
		┌─────────────────┐ ┌─────────┐
		│mem: * ; +memory │ │other: * │
		└─────────────┬───┘ └┬────────┘
		              │      │  ┌─────────────┐
		              │  ┌───┘  │value: Const │
		              │  │      └─┬───────────┘
		              │  │  ┌─────┘
		              │  │  │
		             ┌▼──▼──▼─┐
		             │div: Div│
		             └────────┘""")
	public static Optional<DivByConstPattern.Match> divByConst(Node node) {
		return DivByConstPattern.match(node);
	}

	@FiAscii("""
		┌─────────────────┐ ┌─────────┐
		│mem: * ; +memory │ │other: * │
		└─────────────┬───┘ └┬────────┘
		              │      │  ┌─────────────┐
		              │  ┌───┘  │value: Const │
		              │  │      └─┬───────────┘
		              │  │  ┌─────┘
		              │  │  │
		             ┌▼──▼──▼─┐
		             │mod: Mod│
		             └────────┘""")
	public static Optional<ModByConstPattern.Match> modByConst(Node node) {
		return ModByConstPattern.match(node);
	}

	@FiAscii("""
		┌────────────────┐  ┌─────────┐
		│constVal: Const │  │other: * │
		└─────┬──────────┘  └────┬────┘
		      │                  │
		      └──────┬───────────┘
		             │
		         ┌───▼────┐
		         │mul: Mul│
		         └────────┘""")
	public static Optional<TimesConstPattern.Match> timesConst(Node node) {
		return TimesConstPattern.match(node);
	}

	@FiAscii("""
		 ┌───────────┐
		 │ value: *  │
		 └─────┬─────┘
		       │
		       │
		       │
		┌──────▼──────┐
		│ drop: Minus │
		└──────┬──────┘
		       │
		       │
		       │
		┌──────▼──────┐
		│ repl: Minus │
		└─────────────┘""")
	public static Optional<MinusMinusPattern.Match> minusMinus(Node node) {
		return MinusMinusPattern.match(node);
	}

	@FiAscii("""
		┌──────────┐  ┌─────────────┐
		│ value: * │  │ rVal: Const │
		└─────┬────┘  └─┬───────────┘
		      │         │
		      │         │
		      │         │
		      │         │
		     ┌▼─────────▼──┐ ┌─────────────┐
		     │ right: Shrs │ │ lVal: Const │
		     └─────────┬───┘ └┬────────────┘
		               │      │
		               │      │
		               │      │
		            ┌──▼──────▼─┐
		            │ left: Shl │
		            └───────────┘""")
	public static Optional<ShiftRightSignedLeftPattern.Match> shiftRightSignedLeft(Node node) {
		return ShiftRightSignedLeftPattern.match(node);
	}

	@FiAscii("""
		┌──────────┐  ┌─────────────┐
		│ value: * │  │ rVal: Const │
		└─────┬────┘  └─┬───────────┘
		      │         │
		      │         │
		      │         │
		      │         │
		     ┌▼─────────▼─┐  ┌─────────────┐
		     │ right: Shr │  │ lVal: Const │
		     └─────────┬──┘  └┬────────────┘
		               │      │
		               │      │
		               │      │
		            ┌──▼──────▼─┐
		            │ left: Shl │
		            └───────────┘""")
	public static Optional<ShiftRightLeftPattern.Match> shiftRightLeft(Node node) {
		return ShiftRightLeftPattern.match(node);
	}

	@FiAscii("""
		┌──────────┐  ┌───────────┐
		│ value: * │  │ ci: Const │
		└─────┬────┘  └─┬─────────┘
		      │         │
		      │         │
		      │         │
		      │         │
		     ┌▼─────────▼─┐  ┌───────────┐
		     │ inner: Mul │  │ co: Const │
		     └─────────┬──┘  └┬──────────┘
		               │      │
		               │      │
		               │      │
		            ┌──▼──────▼──┐
		            │ outer: Shl │
		            └────────────┘""")
	public static Optional<MulInShiftLeftPattern.Match> mulInShiftLeft(Node node) {
		return MulInShiftLeftPattern.match(node);
	}

	@FiAscii("""
		┌──────────┐  ┌───────────┐
		│ value: * │  │ ci: Const │
		└─────┬────┘  └─┬─────────┘
		      │         │
		      │         │
		      │         │
		      │         │
		     ┌▼─────────▼─┐  ┌───────────┐
		     │ inner: Shl │  │ co: Const │
		     └─────────┬──┘  └┬──────────┘
		               │      │
		               │      │
		               │      │
		            ┌──▼──────▼──┐
		            │ outer: Shl │
		            └────────────┘""")
	public static Optional<DoubleShiftLeftPattern.Match> doubleShiftLeft(Node node) {
		return DoubleShiftLeftPattern.match(node);
	}
}
