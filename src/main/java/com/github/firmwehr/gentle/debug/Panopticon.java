package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.source.Source;
import firm.nodes.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Panopticon {
	private final Source source;
	private final Map<Node, FirmNodeMetadata> metadata;

	public Panopticon(Source source) {
		this.source = source;
		this.metadata = new HashMap<>();
	}

	public void putMetadata(Node node, FirmNodeMetadata metadata) {
		this.metadata.put(node, metadata);
		node.setDebugInfo(metadata.toDebugInfo(source));
	}

	public Optional<FirmNodeMetadata> getMetadata(Node node) {
		return Optional.ofNullable(metadata.get(node));
	}

	public Optional<String> getMetadataString(Node node) {
		return Optional.ofNullable(metadata.get(node)).map(it -> it.toDebugInfoString(source));
	}
}
