package com.github.firmwehr.gentle.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class SymbolTrie<T> {
	
	private final STNode<T> root;
	
	public SymbolTrie() {
		this.root = new STNode<>(Optional.empty());
	}
	
	public void put(String key, T content) {
		this.root.put(key, content);
	}
	
	public STNode<T> getRoot() {
		return this.root;
	}
	
	public static class STNode<T> {
		
		private final Map<Character, STNode<T>> children;
		private Optional<T> content;
		
		public STNode(Optional<T> content) {
			this.children = new HashMap<>();
			this.content = content;
		}
		
		public void put(String key, T content) {
			if (key.length() == 0) {
				this.content = Optional.of(content);
			} else {
				this.getChildren()
						.computeIfAbsent(key.charAt(0), k -> new STNode<>(Optional.empty()))
						.put(key.substring(1), content);
			}
		}
		
		public Map<Character, STNode<T>> getChildren() {
			return this.children;
		}
		
		public Optional<T> getContent() {
			return this.content;
		}
	}
}
