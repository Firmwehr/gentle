package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.codegen.CodePreselectionMatcher;
import firm.nodes.Node;

import java.util.Optional;
import java.util.function.Function;

/**
 * Same as {@link CodePreselectionMatcher.AddressingScheme} but with nodes resolved to boxes.
 */
public record BoxScheme(
	Optional<IkeaNode> base,
	Optional<IkeaNode> index,
	int scale,
	int displacement
) {
	public static BoxScheme fromAddressingScheme(
		CodePreselectionMatcher.AddressingScheme scheme, Function<Node, IkeaNode> mapper
	) {
		return new BoxScheme(scheme.base().map(mapper), scheme.index().map(mapper), scheme.scale(),
			(int) /* code selection already checked size of displacement */ scheme.displacement());
	}
}
