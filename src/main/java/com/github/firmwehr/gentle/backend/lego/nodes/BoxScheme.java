package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.codegen.CodePreselectionMatcher;
import firm.nodes.Node;

import java.util.Optional;
import java.util.function.Function;

/**
 * Same as {@link CodePreselectionMatcher.AddressingScheme} but with nodes resolved to boxes.
 */
public record BoxScheme(
	Optional<LegoNode> base,
	Optional<LegoNode> index,
	int scale,
	int displacement
) {
	public static BoxScheme fromAddressingScheme(
		CodePreselectionMatcher.AddressingScheme scheme, Function<Node, LegoNode> mapper
	) {
		// int casts are safe, preselection already checked them
		return new BoxScheme(scheme.base().map(mapper), scheme.index().map(mapper), (int) scheme.scale(),
			(int) scheme.displacement());
	}
}
