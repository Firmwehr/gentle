package com.github.firmwehr.gentle.firm;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.firm.construction.StdLibEntity;
import com.github.firmwehr.gentle.output.Logger;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.Relation;
import firm.nodes.Address;
import firm.nodes.Call;
import firm.nodes.Const;
import firm.nodes.Node;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Util {

	private static final Logger LOGGER = new Logger(Util.class);

	private Util() {
		throw new UnsupportedOperationException("No instantiation");
	}

	/**
	 * Exchanges the victim node by murderer, taking care of mode changes.
	 *
	 * @param victim the victim to replace
	 * @param murderer the murderer to replace the victim with
	 */
	public static void exchange(Node victim, Node murderer) {
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
	public static void replace(Node node, Node previousMemory, Node replacement) {
		for (BackEdges.Edge out : BackEdges.getOuts(node)) {
			if (out.node.getMode().equals(Mode.getM())) {
				exchange(out.node, previousMemory);
			} else {
				exchange(out.node, replacement);
			}
		}
	}

	public static Stream<Node> predsStream(Node node) {
		return StreamSupport.stream(node.getPreds().spliterator(), false);
	}

	public static Stream<Node> outsStream(Node node) {
		return StreamSupport.stream(BackEdges.getOuts(node).spliterator(), false).map(edge -> edge.node);
	}

	public static boolean isAllocCall(Call call) {
		return ((Address) call.getPtr()).getEntity().equals(StdLibEntity.ALLOCATE.getEntity());
	}

	public static Relation invert(Relation relation) {
		return switch (relation) {
			case False -> Relation.True;
			case Equal -> Relation.UnorderedLessGreater;
			case Less -> Relation.GreaterEqual;
			case Greater -> Relation.LessEqual;
			case LessEqual -> Relation.Greater;
			case GreaterEqual -> Relation.Less;
			case LessGreater, UnorderedLessGreater -> Relation.Equal;
			case True -> Relation.False;
			default -> throw new InternalCompilerException("Unknown relation " + relation);
		};
	}
}
