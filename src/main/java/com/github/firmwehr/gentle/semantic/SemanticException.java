package com.github.firmwehr.gentle.semantic;

import com.github.firmwehr.gentle.source.Source;
import com.github.firmwehr.gentle.source.SourceSpan;
import com.github.firmwehr.gentle.util.Pair;

import java.util.List;
import java.util.Optional;

public class SemanticException extends Exception {
	private final Source source;
	private final Optional<String> description;
	private final List<Pair<SourceSpan, String>> annotations;

	public SemanticException(Source source, String description) {
		this.source = source;
		this.description = Optional.of(description);
		this.annotations = List.of();
	}

	public SemanticException(Source source, List<Pair<SourceSpan, String>> annotations) {
		this.source = source;
		this.description = Optional.empty();
		this.annotations = annotations;
	}

	public SemanticException(Source source, SourceSpan location, String description) {
		this(source, List.of(new Pair<>(location, description)));
	}

	public SemanticException(
		Source source, SourceSpan location1, String description1, SourceSpan location2, String description2
	) {
		this(source, List.of(new Pair<>(location1, description1), new Pair<>(location2, description2)));
	}


	public SemanticException(
		Source source,
		SourceSpan location1,
		String description1,
		SourceSpan location2,
		String description2,
		SourceSpan location3,
		String description3
	) {
		this(source, List.of(new Pair<>(location1, description1), new Pair<>(location2, description2),
			new Pair<>(location3, description3)));
	}

	@Override
	public String getMessage() {
		StringBuilder builder = new StringBuilder();

		builder.append("Semantic error");

		description.ifPresent(s -> builder.append("\n").append(s));

		for (Pair<SourceSpan, String> annotation : annotations) {
			builder.append("\n")
				.append(source.formatMessagePointingTo(annotation.first().startOffset(), annotation.second()));
		}

		return builder.toString();
	}
}
