package com.github.firmwehr.gentle.util;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Optional;
import java.util.PrimitiveIterator;

/**
 * This class implements a Trie with delayed traversal, meaning that you can start a key loopup without having the
 * entire key ready yet. Instead the key can be submitted one by one and the caller is notified about the traversal
 * process.
 *
 * @param <V> Type of values to look up.
 */
public class TokenTrie<V> {

	private final Node<V> root = new Node<>();

	public void put(String key, V value) {
		root.put(key.codePoints().iterator(), value, key);
	}

	/**
	 * Starts a new traversal at the root node.
	 *
	 * @return State of interactive traversal.
	 */
	public TraversalOperation startTraversal() {
		return new TraversalOperation();
	}

	private static class Node<V> {
		private final HashMap<Integer, Node<V>> children = new HashMap<>();
		private Optional<V> value;

		public Node() {
			this.value = Optional.empty();
		}

		public Node(V value) {
			this.value = Optional.of(value);
		}

		public void put(PrimitiveIterator.OfInt it, V passingValue, String key) {
			if (it.hasNext()) {
				// take one and advanced to next node
				int cp = it.nextInt();
				children.computeIfAbsent(cp, _ignore -> new Node<>()).put(it, passingValue, key);
			} else {
				// end of input reached, current node must be empty or else we have duplicated keys
				Preconditions.checkArgument(value.isEmpty(), "duplicated key: " + key);
				value = Optional.of(passingValue);
			}
		}
	}

	/**
	 * Represents a started trie traversal call {@link #advance(int)} to advanced the traversal down the trie and use
	 * {@link #current()} to access the last traversed element, once the traversal has reached a dead end.
	 */
	public class TraversalOperation {
		private Node<V> current = TokenTrie.this.root;

		/**
		 * Advances the traversal by the given symbol.
		 *
		 * @param codepoint The next codepoint for the traversal.
		 *
		 * @return {@code true} if the traversal could be advanced or {@code false} if the given codepoint does not
		 * 	represent a valid path, after which {@link #current()} can be called to retrieve the current detected
		 * 	element.
		 */
		public boolean advance(int codepoint) {
			var next = current.children.get(codepoint);
			if (next == null) {
				// end of traversal
				return false;
			}

			// advance to next token
			current = next;
			return true;
		}

		/**
		 * @return The current element of the traversal process. Is only set if at least one call to {@link
		 *    #advance(int)} returned {@code}.
		 */
		public Optional<V> current() {
			return current.value;
		}
	}
}
