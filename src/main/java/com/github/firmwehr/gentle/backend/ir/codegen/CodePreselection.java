package com.github.firmwehr.gentle.backend.ir.codegen;

import firm.nodes.Node;

import java.util.Optional;

public interface CodePreselection {
	Optional<CodePreselectionMatcher.AddressingScheme> scheme(Node n);

	boolean hasBeenReplaced(Node n);
}
