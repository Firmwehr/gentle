package com.github.firmwehr.gentle.firm.optimization.memory;

import firm.nodes.Add;
import firm.nodes.Const;
import firm.nodes.Load;
import firm.nodes.Node;
import firm.nodes.Proj;
import firm.nodes.Store;

import java.util.Optional;

/**
 * A field of an object, identified by a base pointer and the offset within the object.
 */
public record LocalAddress(
	Proj pointer,
	long offset
) {

	/**
	 * Returns a {@link LocalAddress} if the given node matches the required layout.
	 * <p>
	 * Valid layouts are direct Projs (a pointer with offset 0), or Adds of a Proj and a Const.
	 *
	 * @param ptr the Node provided by either {@link Load#getPtr()} or {@link Store#getPtr()}.
	 */
	private static Optional<LocalAddress> fromPtr(Node ptr) {
		if (ptr instanceof Proj proj) {
			return Optional.of(new LocalAddress(proj, 0)); // 0th field in class
		} else if (ptr instanceof Add add) {
			if (add.getLeft() instanceof Proj proj && add.getRight() instanceof Const val) {
				return Optional.of(new LocalAddress(proj, val.getTarval().asLong()));
			} else if (add.getRight() instanceof Proj proj && add.getLeft() instanceof Const val) {
				return Optional.of(new LocalAddress(proj, val.getTarval().asLong()));
			}
		}
		return Optional.empty();
	}

	public static Optional<LocalAddress> fromLoad(Load load) {
		return fromPtr(load.getPtr());
	}

	public static Optional<LocalAddress> fromStore(Store store) {
		return fromPtr(store.getPtr());
	}

	public boolean pointerEquals(LocalAddress other) {
		return pointer().equals(other.pointer());
	}
}
