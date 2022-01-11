package com.github.firmwehr.gentle.debug;

import firm.DebugInfo;
import firm.nodes.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Panopticon {
	private static final Panopticon instance = new Panopticon();

	private final Map<Node, FirmNodeMetadata> metadata;
	private final Map<Node, AllocationInfo> allocationInfos;

	private Panopticon() {
		this.metadata = new HashMap<>();
		this.allocationInfos = new HashMap<>();
	}

	public void putMetadata(Node node, FirmNodeMetadata metadata) {
		this.metadata.put(node, metadata);
		node.setDebugInfo(DebugInfo.createInfo(metadata.toString(), 0, 0));
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

	public static Panopticon getInstance() {
		return instance;
	}
}
