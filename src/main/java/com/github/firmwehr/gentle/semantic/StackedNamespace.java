package com.github.firmwehr.gentle.semantic;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.source.Source;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.util.JImmutables;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * A namespace that maps strings to some value. This differs from a normal map, as it allows the user to {@link
 * #enterScope() enter} and {@link #leaveScope() leave} scopes. When you enter a new scope, all your existing mappings
 * are kept. Wenn you leave a scope, all mappings you added using {@link #put(Ident, Object)} while in that scope will
 * be removed.
 * <p>
 * This implementation still allows for fast lookup by using a persistent map.
 *
 * @param <T> the type of the elements contained within
 */
public class StackedNamespace<T> {

	private final Source source;
	private final Deque<JImmutableMap<String, Entry<T>>> scopes;

	public StackedNamespace(Source source) {
		this.source = source;
		this.scopes = new ArrayDeque<>();

		this.scopes.addFirst(JImmutables.map());
	}

	public void enterScope() {
		scopes.addFirst(scopes.peekFirst());
	}

	public void leaveScope() {
		if (scopes.size() <= 1) {
			throw new InternalCompilerException("tried to leave outermost scope!");
		}
		scopes.pollFirst();
	}

	public Optional<T> getOpt(Ident name) {
		if (scopes.isEmpty()) {
			throw new InternalCompilerException("scope stack is empty");
		}
		return Optional.ofNullable(scopes.peekFirst().get(name.ident())).map(Entry::value);
	}

	public T get(Ident name) throws SemanticException {
		return getOpt(name).orElseThrow(() -> new SemanticException(source, name.sourceSpan(), "unknown name"));
	}

	public void put(Ident name, T t) throws SemanticException {
		if (scopes.isEmpty()) {
			throw new InternalCompilerException("scope stack is empty");
		}

		JImmutableMap<String, Entry<T>> currentMap = scopes.peekFirst();
		Optional<Entry<T>> existing = Optional.ofNullable(currentMap.get(name.ident()));
		if (existing.isPresent()) {
			Ident existingName = existing.get().ident();
			throw new SemanticException(source, name.sourceSpan(), "invalid name", existingName.sourceSpan(),
				"already defined here");
		} else {
			// Same slot in scopes, so delete the old one!
			scopes.pollFirst();
			scopes.addFirst(currentMap.assign(name.ident(), new Entry<>(name, t)));
		}
	}

	public boolean contains(String name) {
		if (scopes.isEmpty()) {
			return false;
		}
		return scopes.peekFirst().get(name) != null;
	}

	private record Entry<T>(
		Ident ident,
		T value
	) {
	}
}
