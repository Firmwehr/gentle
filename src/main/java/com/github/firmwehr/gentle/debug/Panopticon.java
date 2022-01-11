package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.source.Source;
import firm.nodes.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Panopticon {
	private final Source source;
	private final Map<Node, FirmNodeMetadata> metadata;
	private final Map<Node, AllocationInfo> allocationInfos;

	public Panopticon(Source source) {
		this.source = source;
		this.metadata = new HashMap<>();
		this.allocationInfos = new HashMap<>();
	}

	public void putMetadata(Node node, FirmNodeMetadata metadata) {
		this.metadata.put(node, metadata);
		node.setDebugInfo(metadata.toDebugInfo(source));
	}

	public Optional<FirmNodeMetadata> getMetadata(Node node) {
		return Optional.ofNullable(metadata.get(node));
	}

	public void putAllocationInfo(Node node, AllocationInfo info) {
		allocationInfos.put(node, info);
	}

	public Optional<AllocationInfo> getAllocationInfo(Node node) {
		return Optional.ofNullable(allocationInfos.get(node));
	}
}
