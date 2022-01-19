package com.github.firmwehr.gentle.backend.ir.codegen;

import firm.nodes.Node;

import java.util.Optional;

public interface CodePreselection {
	Optional<CodePreselectionMatcher.AddressingScheme> scheme(Node n);

	boolean hasBeenReplaced(Node n);

	int replacedSubtrees();

	/**
	 * This is a dummy implementation of a preselection stage. It's primary purpose is to stay compatible with Molki,
	 * since Molki doesn't understand complex addressing schemes.
	 */
	record Dummy() implements CodePreselection {

		@Override
		public Optional<CodePreselectionMatcher.AddressingScheme> scheme(Node n) {
			return Optional.empty();
		}

		@Override
		public boolean hasBeenReplaced(Node n) {
			return false;
		}

		@Override
		public int replacedSubtrees() {
			return 0;
		}
	}
}
