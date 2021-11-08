package com.github.firmwehr.gentle.testutil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@SuppressWarnings("ClassCanBeRecord")
public class EqualityChecker<T> implements Predicate<T> {
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	private static final Map<Class<?>, List<MethodHandle>> ACCESSOR_CACHE = new HashMap<>();
	private final T expected;
	private final Set<Class<?>> exceptTypes;
	private final boolean deepScanRecords;
	private final boolean deepScanCollections;

	public EqualityChecker(
		T expected, Set<Class<?>> exceptTypes, boolean deepScanRecords, boolean deepScanCollections
	) {
		this.expected = expected;
		this.exceptTypes = exceptTypes;
		this.deepScanRecords = deepScanRecords;
		this.deepScanCollections = deepScanCollections;
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
		if (!this.expected.getClass().isRecord() || !actual.getClass().isRecord()) {
			return this.expected.equals(actual);
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
		if (this.deepScanCollections && expectedAttribute instanceof Collection a &&
			actualAttribute instanceof Collection b) {
			return collectionsEqual(a, b);
		}
		// we need to handle Optional separately
		if (this.deepScanRecords && expectedAttribute instanceof Optional<?> eO &&
			actualAttribute instanceof Optional<?> aO) {
			if (eO.isEmpty()) {
				return aO.isEmpty();
			}
			Object nonOptionalExpectedAttribute = eO.get();
			if (!nonOptionalExpectedAttribute.getClass().isRecord()) {
				return nonOptionalExpectedAttribute.equals(aO.orElse(null));
			}
			return new EqualityChecker<>(nonOptionalExpectedAttribute, this.exceptTypes, true,
				this.deepScanCollections).isEqual(aO.orElse(null));
		}
		// if we don't need to deeply scan records, we can just use normal equals here
		if (!this.deepScanRecords ||
			!(expectedAttribute.getClass().isRecord() && expectedAttribute.getClass() == actualAttribute.getClass())) {
			return expectedAttribute.equals(actualAttribute);
		}
		// otherwise, we just run a check again on the attribute
		return new EqualityChecker<>(expectedAttribute, this.exceptTypes, true, this.deepScanCollections).isEqual(
			actualAttribute);
	}

	private boolean collectionsEqual(Collection<?> a, Collection<?> b) {
		if (a == b) {
			return true;
		}
		if (a.size() != b.size()) {
			return false;
		}
		//noinspection SwitchStatementWithTooFewBranches
		return switch (a) {
			case List<?> list && b instanceof List<?> bList -> listsEqual(list, bList);
			default -> a.equals(b);
		};
	}

	private boolean listsEqual(List<?> a, List<?> b) {
		for (Iterator<?> aIt = a.iterator(), bIt = b.iterator(); aIt.hasNext() && bIt.hasNext(); ) {
			Object aObj = aIt.next();
			if (!new EqualityChecker<>(aObj, this.exceptTypes, this.deepScanRecords, this.deepScanCollections).isEqual(
				bIt.next())) {
				return false;
			}
		}
		return true;
	}

	private static List<MethodHandle> createAccessors(Class<?> recordType) {
		return Arrays.stream(recordType.getRecordComponents())
			.map(RecordComponent::getAccessor)
			.map(EqualityChecker::lookupSafe)
			.toList();
	}

	private static MethodHandle lookupSafe(Method method) {
		try {
			// use the hammer to access methods in non-public records
			var privateLookup = MethodHandles.privateLookupIn(method.getDeclaringClass(), LOOKUP);
			return privateLookup.unreflect(method);
		} catch (IllegalAccessException e) {
			throw new AssertionError("Accessed " + method + " which is not accessible", e);
		}
	}
}
