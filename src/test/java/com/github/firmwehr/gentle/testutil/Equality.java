package com.github.firmwehr.gentle.testutil;

import com.github.firmwehr.gentle.source.SourceSpan;
import com.google.common.base.Preconditions;
import org.assertj.core.api.Condition;

import java.util.Set;

public final class Equality {
	private Equality() {

	}

	public static <T> Condition<? super T> equalExcept(
		T obj, Set<Class<?>> attributeTypes, boolean deepScanRecords, boolean deepScanCollections
	) {
		Preconditions.checkArgument(obj.getClass().isRecord(), "must be a record type comparison");
		return new Condition<>(new EqualityChecker<>(obj, attributeTypes, deepScanRecords, deepScanCollections),
			"a record ignoring following attributes " + (deepScanRecords ? "deeply " : "") + attributeTypes + "\n  " +
				obj);
	}

	public static <T> Condition<? super T> equalExcept(
		T obj, Class<?> attributeType, boolean deepScanRecords, boolean deepScanCollections
	) {
		return equalExcept(obj, Set.of(attributeType), deepScanRecords, deepScanCollections);
	}

	public static <T> Condition<? super T> equalExceptSourcePosition(T obj) {
		return equalExcept(obj, SourceSpan.class, true, true);
	}
}
