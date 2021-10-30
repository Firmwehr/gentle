package com.github.firmwehr.gentle.testutil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@SuppressWarnings("ClassCanBeRecord")
public class EqualityChecker<T> implements Predicate<T> {
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();
	private static final Map<Class<?>, List<MethodHandle>> ACCESSOR_CACHE = new HashMap<>();
	private final T expected;
	private final Set<Class<?>> exceptTypes;
	private final boolean deep;

	public EqualityChecker(T expected, Set<Class<?>> exceptTypes, boolean deep) {
		this.expected = expected;
		this.exceptTypes = exceptTypes;
		this.deep = deep;
	}

	@Override
	public boolean test(T actual) {
		return isEqual(actual);
	}

	public boolean isEqual(T actual) {
		if (this.expected == actual) {
			return true;
		}
		if (actual == null) {
			return false;
		}
		var handles = ACCESSOR_CACHE.computeIfAbsent(this.expected.getClass(), EqualityChecker::createAccessors);
		for (MethodHandle handle : handles) {
			if (this.exceptTypes.contains(handle.type().returnType())) {
				continue; // ignore this one
			}
			if (!isEqualAttribute(handle, actual)) {
				return false;
			}
		}
		return true;
	}

	private boolean isEqualAttribute(MethodHandle handle, T actual) {
		Object expectedAttribute;
		Object actualAttribute;
		try {
			expectedAttribute = handle.invoke(this.expected);
			actualAttribute = handle.invoke(actual);
		} catch (Throwable e) {
			throw new AssertionError("accessor threw exception", e);
		}
		if (expectedAttribute == null || actualAttribute == null) {
			// if both are null, we consider them equal, if only one is null, we consider them not equal
			return expectedAttribute == actualAttribute;
		}
		// if we don't need to deeply scan records, we can just use normal equals here
		if (!this.deep ||
			!(expectedAttribute.getClass().isRecord() && expectedAttribute.getClass() == actualAttribute.getClass())) {
			return expectedAttribute.equals(actualAttribute);
		}
		// otherwise, we just run a check again on the attribute
		return new EqualityChecker<>(expectedAttribute, this.exceptTypes, true).isEqual(actualAttribute);
	}

	private static List<MethodHandle> createAccessors(Class<?> recordType) {
		return Arrays.stream(recordType.getRecordComponents())
			.map(RecordComponent::getAccessor)
			.map(EqualityChecker::lookupSafe)
			.toList();
	}

	private static MethodHandle lookupSafe(Method method) {
		try {
			return LOOKUP.unreflect(method);
		} catch (IllegalAccessException e) {
			throw new AssertionError("Accessed " + method + " which is not accessible", e);
		}
	}
}
