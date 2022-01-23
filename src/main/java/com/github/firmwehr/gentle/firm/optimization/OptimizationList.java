package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.fiascii.asciiart.generating.BaseMatch;
import firm.Graph;
import firm.nodes.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class OptimizationList {

	private final List<Entry<?>> optimizations;

	private OptimizationList(List<Entry<?>> optimizations) {
		this.optimizations = List.copyOf(optimizations);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final List<Entry<?>> optimizations;

		private Builder() {
			this.optimizations = new ArrayList<>();
		}

		public <T extends BaseMatch> Builder addStep(Function<Node, Optional<T>> matcher, Action<T> action) {
			this.optimizations.add(new Entry<>(matcher, action));
			return this;
		}

		public OptimizationList build() {
			return new OptimizationList(this.optimizations);
		}
	}

	public boolean optimize(Node node, Graph graph, Node block) {
		for (Entry<?> optimization : this.optimizations) {
			if (apply(optimization, node, graph, block)) {
				return true;
			}
		}
		return false;
	}

	private <T extends BaseMatch> boolean apply(Entry<T> entry, Node node, Graph graph, Node block) {
		return entry.function().apply(node).map(match -> entry.action().accept(match, graph, block)).orElse(false);
	}

	public interface Action<T extends BaseMatch> {
		boolean accept(T match, Graph graph, Node block);
	}

	private record Entry<T extends BaseMatch>(
		Function<Node, Optional<T>> function,
		Action<T> action
	) {

	}
}
