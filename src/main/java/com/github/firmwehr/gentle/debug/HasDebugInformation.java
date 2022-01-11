package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public interface HasDebugInformation {

	/**
	 * @return some extra debug information about this element
	 *
	 * @implNote the default implementation returns an empty string
	 */
	default String additionalInfo() {
		return "";
	}

	/**
	 * @return the span in the source code
	 */
	Optional<SourceSpan> debugSpan();
}
