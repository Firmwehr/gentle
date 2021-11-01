package com.github.firmwehr.gentle.semantic;

import com.github.firmwehr.gentle.source.Source;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public class SemanticException extends Exception {
	private final Source source;
	private final SourceSpan errorLocation;
	private final String errorDescription;
	private final Optional<SourceSpan> referenceLocation;
	private final Optional<String> referenceDescription;

	public SemanticException(Source source, SourceSpan errorLocation, String errorDescription) {
		this.source = source;
		this.errorLocation = errorLocation;
		this.errorDescription = errorDescription;
		referenceLocation = Optional.empty();
		referenceDescription = Optional.empty();
	}

	public SemanticException(
		Source source,
		SourceSpan errorLocation,
		String errorDescription,
		SourceSpan referenceLocation,
		String referenceDescription
	) {
		this.source = source;
		this.errorLocation = errorLocation;
		this.errorDescription = errorDescription;
		this.referenceLocation = Optional.of(referenceLocation);
		this.referenceDescription = Optional.of(referenceDescription);
	}

	@Override
	public String getMessage() {
		if (referenceLocation.isPresent() && referenceDescription.isPresent()) {
			return source.formatErrorWithReferenceAt("Semantic error", errorLocation.startOffset(), errorDescription,
				referenceLocation.get().startOffset(), referenceDescription.get());
		} else {
			return source.formatErrorAt("Semantic error", errorLocation.startOffset(), errorDescription);
		}
	}
}
