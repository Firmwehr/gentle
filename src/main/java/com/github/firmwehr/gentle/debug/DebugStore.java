package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.source.Source;
import firm.nodes.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Stores debug information for firm nodes so we can reproduce their information later (e.g. in assembly output or
 * graphs).
 * <br>
 * <p><sub>Debug ain't cheap! You better bring quite a few coins!</sub>
 */
public class DebugStore {
	private final Source source;
	private final Map<Node, FirmNodeMetadata> metadata;

	public DebugStore(Source source) {
		this.source = source;
		this.metadata = new HashMap<>();
	}

	public void putMetadata(Node node, FirmNodeMetadata metadata) {
		this.metadata.put(node, metadata);
		node.setDebugInfo(metadata.toDebugInfo(source));
	}

	public Optional<String> getMetadataString(Node node) {
		return Optional.ofNullable(metadata.get(node)).map(it -> it.toDebugInfoString(source));
	}
}
