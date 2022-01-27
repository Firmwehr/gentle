package com.github.firmwehr.gentle.firm.optimization.callgraph;

import com.github.firmwehr.gentle.firm.construction.StdLibEntity;
import com.google.common.graph.ImmutableNetwork;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.Network;
import com.google.common.graph.NetworkBuilder;
import firm.Entity;
import firm.Graph;
import firm.nodes.Address;
import firm.nodes.Call;
import firm.nodes.Load;
import firm.nodes.NodeVisitor;
import firm.nodes.Store;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage") // that should be ENCOURAGED instead of warning us...
public final class CallGraph {
	private final Network<Entity, Call> calledMethods;
	private final Map<Entity, Set<Effect>> effectCache;

	private CallGraph(Network<Entity, Call> calledMethods) {
		this.calledMethods = ImmutableNetwork.copyOf(calledMethods);
		this.effectCache = new HashMap<>();
	}

	public static CallGraph create(Iterable<Graph> graphs) {
		MutableNetwork<Entity, Call> network = NetworkBuilder.directed() //
			.allowsSelfLoops(true) //
			.allowsParallelEdges(true) //
			.build();
		for (Graph graph : graphs) {
			buildNode(network, graph);
		}
		return new CallGraph(network);
	}

	private static void buildNode(MutableNetwork<Entity, Call> network, Graph graph) {
		Entity entity = graph.getEntity();
		graph.walk(new NodeVisitor.Default() {
			@Override
			public void visit(Call node) {
				Address address = (Address) node.getPtr();
				network.addEdge(entity, address.getEntity(), node);
			}
		});
	}

	/**
	 * Updates the outgoing edges of the given graphs and returns the call graph representing the new state.
	 *
	 * @param toUpdate the graphs to update outgoing edges for.
	 *
	 * @return the new call graph state
	 */
	public CallGraph updated(Set<Graph> toUpdate) {
		if (toUpdate.isEmpty()) {
			return this;
		}
		Set<Entity> entitiesToUpdate = toUpdate.stream().map(Graph::getEntity).collect(Collectors.toSet());
		// WHY on earth can't we create a mutable network instance from an immutable one?
		MutableNetwork<Entity, Call> updated = NetworkBuilder.directed()
			.allowsSelfLoops(true)
			.allowsParallelEdges(true)
			.expectedNodeCount(calledMethods.nodes().size()) // maybe,
			.expectedEdgeCount(calledMethods.edges().size()) // maybe this helps a bit...
			.build();
		for (Entity node : calledMethods.nodes()) {
			if (entitiesToUpdate.contains(node)) {
				buildNode(updated, node.getGraph());
			} else {
				// re-insert outgoing edges of this node, as we don't want to update it
				for (Call call : calledMethods.outEdges(node)) {
					updated.addEdge(node, ((Address) call.getPtr()).getEntity(), call);
				}
			}
		}
		return new CallGraph(updated);
	}

	/**
	 * Returns the effects caused by this entity. This includes effects of calls to other entities.
	 *
	 * @param entity the entity to get the effects for
	 *
	 * @return an unmodifiable view of the entity's effects
	 */
	public Set<Effect> effects(Entity entity) {
		Set<Effect> cached = effectCache.get(entity);
		if (cached == null) { // not cached yet
			cached = EnumSet.noneOf(Effect.class);
			// put before collect, avoiding infinite recursion
			effectCache.put(entity, cached);
			collectEffects(entity, cached);
		}
		return Collections.unmodifiableSet(cached);
	}

	private void collectEffects(Entity entity, Set<Effect> cached) {
		if (entity.equals(StdLibEntity.ALLOCATE.getEntity())) {
			cached.add(Effect.ALLOCATE);
		} else if (entity.equals(StdLibEntity.PRINTLN.getEntity()) || entity.equals(StdLibEntity.PUTCHAR.getEntity()) ||
			entity.equals(StdLibEntity.FLUSH.getEntity())) {
			cached.add(Effect.OUTPUT);
		} else if (entity.equals(StdLibEntity.GETCHAR.getEntity())) {
			cached.add(Effect.INPUT);
		} else if (entity.getGraph() != null) {
			entity.getGraph().walk(new NodeVisitor.Default() {
				@Override
				public void visit(Load node) {
					cached.add(Effect.LOAD);
				}

				@Override
				public void visit(Store node) {
					cached.add(Effect.STORE);
				}
			});
			Set<Call> calls = calledMethods.outEdges(entity);
			if (!calls.isEmpty()) {
				cached.add(Effect.CALL);
				calls.stream()
					.map(call -> ((Address) call.getPtr()).getEntity())
					.map(this::effects)
					.forEach(cached::addAll);
			}
		}
	}

	/**
	 * Each graph is only visited after all callees were visited, except for graphs forming a cycle.
	 * <p>
	 * For a cycle with exact one caller, e.g. {@code c -> a -> b -> a}, {@code b} will be visited before {@code a}.
	 * Order is not defined if there is not exactly one call into any graph in the cycle.
	 */
	public void walkPostorder(Consumer<Graph> visitor) {
		List<Graph> out = new ArrayList<>();
		Deque<Entity> nodes = new ArrayDeque<>(calledMethods.nodes());
		Set<Entity> temp = new HashSet<>();

		while (!nodes.isEmpty()) {
			visit(nodes.remove(), temp, out);
		}

		for (Graph graph : out) {
			visitor.accept(graph);
		}
	}

	private void visit(Entity entity, Set<Entity> visited, List<Graph> out) {
		if (!visited.add(entity)) {
			return;
		}
		for (Entity successor : calledMethods.successors(entity)) {
			visit(successor, visited, out);
		}
		if (entity.getGraph() != null) {
			out.add(entity.getGraph());
		}
	}

	public Set<Call> callSitesIn(Graph graph) {
		return calledMethods.outEdges(graph.getEntity());
	}

	@Override
	public String toString() {
		return "CallGraph";
	}

	public enum Effect {
		LOAD, // loads from memory
		STORE, // stores into memory
		ALLOCATE, // allocates memory
		INPUT, // the 'I' in IO
		OUTPUT, // the 'O' in IO
		CALL // calls other functions
	}
}
