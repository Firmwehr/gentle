package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.firm.optimization.callgraph.CallGraph;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.GraphDumper;
import com.github.firmwehr.gentle.util.Pair;
import firm.BackEdges;
import firm.Entity;
import firm.Graph;
import firm.MethodType;
import firm.Type;
import firm.nodes.Address;
import firm.nodes.Call;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Proj;
import firm.nodes.Start;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public class UnusedParameterOptimization {
	private static final Logger LOGGER = new Logger(UnusedParameterOptimization.class, Logger.LogLevel.DEBUG);

	private static final int CALL_ARGUMENT_OFFSET = Start.pnTArgs; // [0: Proj M, 1: Address, 2...: Arguments...]
	private static final int ADDRESS_PRED_INDEX = 1;
	private static final Proj UNUSED_PROJ = null;
	private final CallGraph graph;
	private final Map<Entity, CallRewrite> callRewrites;
	private final List<DelayedRewriteTask> delayedTasks = new ArrayList<>();
	private final Set<Graph> modified = new HashSet<>();

	public UnusedParameterOptimization(CallGraph graph) {
		this.graph = graph;
		this.callRewrites = new HashMap<>();
	}

	public static GraphOptimizationStep<CallGraph> unusedParameterOptimization() {
		return GraphOptimizationStep.<CallGraph>builder()
			.withDescription("UnusedParameterOptimization")
			.withOptimizationFunction(callGraph -> new UnusedParameterOptimization(callGraph).optimize())
			.build();
	}

	public boolean optimize() {
		this.graph.walkPostorder(g -> {
			LOGGER.debug("Scanning %s", g.getEntity().getLdName());
			boolean localChange = false;
			BackEdges.enable(g);
			// replace all arguments in calls in this graph if they aren't used in their graph
			// before checking the parameter usage of this graph to avoid unneeded forwarding
			replaceUnusedForCalls(g);

			int used = 0;
			int lastUsed = -1;
			MethodType type = (MethodType) g.getEntity().getType();
			Proj[] projections = new Proj[type.getNParams()];
			for (BackEdges.Edge edge : BackEdges.getOuts(g.getArgs())) { // Proj <Type> Arg <Index>
				if (edge.node instanceof Proj proj) {
					// TODO: if an argument is only passed to a method in a circle
					// in the call graph, and it's only used to call *this* method again,
					// we can mark it as unused and drop it
					projections[proj.getNum()] = proj;
					used++;
					lastUsed = Math.max(proj.getNum(), lastUsed);
				}
			}
			int[] reorder = new int[used];
			for (int i = 0; i < used; i++) {
				if (projections[i] == UNUSED_PROJ) {
					projections[i] = projections[lastUsed];
					reorder[i] = projections[i].getNum(); // store old position
					projections[i].setNum(i); // set new position
					projections[lastUsed] = UNUSED_PROJ;
					while (lastUsed >= 0 && projections[lastUsed] == UNUSED_PROJ) {
						lastUsed--;
					}
				} else {
					reorder[i] = i;
				}
			}
			if (used != type.getNParams()) {
				localChange = true;
				MethodType newType = updateType(type, reorder, used);
				g.getEntity().setType(newType);
				this.callRewrites.put(g.getEntity(), new IndexedRewrite(reorder, used));
			} else {
				this.callRewrites.put(g.getEntity(), NoRewrite.INSTANCE);
			}

			BackEdges.disable(g);
			if (localChange) {
				this.modified.add(g);
			}
		});
		postProcessDelayedTasks();
		for (Graph g : this.modified) {
			GraphDumper.dumpGraph(g, "replace-unused");
		}
		System.out.println(this.modified);
		return !this.modified.isEmpty();
	}

	private void postProcessDelayedTasks() {
		for (DelayedRewriteTask delayedTask : delayedTasks) {
			CallRewrite rewrite = callRewrites.get(delayedTask.entity());
			if (rewrite == null) {
				throw new IllegalStateException(delayedTask.entity() + " was not visited");
			}
			System.out.println("delayed rewrite for " + delayedTask.entity() + " in " + delayedTask.call().getGraph());
			Call newCall = rewrite.rewrite(delayedTask.call());
			if (newCall != delayedTask.call()) {
				Graph.exchange(delayedTask.call(), newCall);
				this.modified.add(delayedTask.call().getGraph());
			}
		}
	}

	private MethodType updateType(MethodType oldType, int[] reorder, int used) {
		Type[] parameterTypes = new Type[used];
		for (int i = 0; i < used; i++) {
			parameterTypes[i] = oldType.getParamType(reorder[i]);
		}
		Type[] resType;
		if (oldType.getNRess() > 0) {
			resType = new Type[]{oldType.getResType(0)};
		} else {
			resType = new Type[0];
		}
		return new MethodType(parameterTypes, resType);
	}

	private void replaceUnusedForCalls(Graph graph) {
		List<Pair<Call, Call>> toExchange = new ArrayList<>();
		graph.walk(new NodeVisitor.Default() {
			@Override
			public void visit(Call node) {
				Address address = (Address) node.getPred(1);
				if (address.getEntity().getGraph() == null) {
					return;
				}
				CallRewrite rewrite = callRewrites.get(address.getEntity());
				if (rewrite == null) {
					delayedTasks.add(new DelayedRewriteTask(node, address.getEntity()));
				} else {
					Call newCall = rewrite.rewrite(node);
					if (newCall != node) {
						modified.add(graph);
						toExchange.add(new Pair<>(node, newCall));
					}
				}
			}
		});
		for (Pair<Call, Call> pair : toExchange) {
			Graph.exchange(pair.first(), pair.second());
		}
	}

	record DelayedRewriteTask(
		Call call,
		Entity entity
	) {

	}

	private interface CallRewrite {
		Call rewrite(Call call);
	}

	private enum NoRewrite implements CallRewrite {
		INSTANCE;

		@Override
		public Call rewrite(Call call) {
			return call;// nop
		}
	}

	private record IndexedRewrite(
		int[] indexes,
		int usedCount
	) implements CallRewrite {

		@Override
		public Call rewrite(Call call) {
			int predCount = call.getPredCount();
			// "restore" current argument array from firm
			Node[] arguments = new Node[predCount - CALL_ARGUMENT_OFFSET];
			for (int i = CALL_ARGUMENT_OFFSET; i < predCount; i++) {
				arguments[i - CALL_ARGUMENT_OFFSET] = call.getPred(i);
			}
			for (int i = 0; i < usedCount; i++) {
				int index = indexes[i];
				if (index != i) {
					arguments[i] = arguments[index];
				}
			}
			Node[] rewrittenArguments = Arrays.copyOfRange(arguments, 0, usedCount);
			Type newType = ((Address) call.getPred(ADDRESS_PRED_INDEX)).getEntity().getType();
			return (Call) call.getGraph()
				.newCall(call.getBlock(), call.getMem(), call.getPtr(), rewrittenArguments, newType);
		}
	}

	private static String methodTypeToSignature(MethodType type) {
		StringJoiner joiner = new StringJoiner(",", "(", ")");
		for (int i = 0; i < type.getNParams(); i++) {
			joiner.add(type.getParamType(i).toString());
		}
		return joiner + (type.getNRess() > 0 ? type.getResType(0).toString() : "V");
	}
}
