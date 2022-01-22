package com.github.firmwehr.gentle.util;

import java.util.Objects;

public class Mut<T> {
	private T t;

	public Mut(T t) {
		this.t = t;
	}

	public T get() {
		return t;
	}

	public void set(T t) {
		this.t = t;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Mut<?> mut = (Mut<?>) o;
		return Objects.equals(t, mut.t);
	}

	@Override
	public int hashCode() {
		return Objects.hash(t);
	}
}
