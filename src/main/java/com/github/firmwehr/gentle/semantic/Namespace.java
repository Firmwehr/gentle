package com.github.firmwehr.gentle.semantic;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.source.Source;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A map from string to element that rejects duplicate entries by throwing a {@link SemanticException}.
 *
 * @param <T> the type of the elements within
 */
public class Namespace<T> {
	private final Source source;
	private final Map<String, Entry<T>> content;

	public Namespace(Source source) {
		this.source = source;
		content = new HashMap<>();
	}

	public Optional<T> getOpt(Ident name) {
		return Optional.ofNullable(content.get(name.ident())).map(Entry::value);
	}

	public T get(Ident name) throws SemanticException {
		return getOpt(name).orElseThrow(() -> new SemanticException(source, name.sourceSpan(), "unknown name"));
	}

	public void put(Ident name, T t) throws SemanticException {
		Optional<Entry<T>> existing = Optional.ofNullable(content.get(name.ident()));
		if (existing.isPresent()) {
			Ident existingName = existing.get().name;
			throw new SemanticException(source, name.sourceSpan(), "invalid name", existingName.sourceSpan(),
				"already defined here");
		} else {
			content.put(name.ident(), new Entry<>(name, t));
		}
	}

	public boolean contains(String name) {
		return content.containsKey(name);
	}

	public Set<T> getAll() {
		return content.values().stream().map(Entry::value).collect(Collectors.toSet());
	}

	private static record Entry<T>(
		Ident name,
		T value
	) {
	}
}
