package com.github.firmwehr.gentle.semantic;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.source.Source;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.util.JImmutables;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

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
			throw new IllegalStateException("Tried to leave outermost scope!");
		}
		scopes.pollFirst();
	}

	public Optional<T> getOpt(Ident name) {
		if (scopes.isEmpty()) {
			throw new IllegalStateException("Scope stack is empty");
		}
		return Optional.ofNullable(scopes.peekFirst().get(name.ident())).map(Entry::value);
	}

	public T get(Ident name) throws SemanticException {
		return getOpt(name).orElseThrow(() -> new SemanticException(source, name.sourceSpan(), "unknown name"));
	}

	public void put(Ident name, T t) throws SemanticException {
		if (scopes.isEmpty()) {
			throw new IllegalStateException("Scope stack is empty");
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

	private record Entry<T>(
		Ident ident,
		T value
	) {
	}
}
