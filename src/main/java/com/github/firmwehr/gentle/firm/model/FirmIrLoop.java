package com.github.firmwehr.gentle.firm.model;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.firm.GentleBindings;
import com.sun.jna.Pointer;
import firm.bindings.binding_firm_common;
import firm.bindings.binding_irnode;
import firm.bindings.binding_irnode.firm_kind;
import firm.nodes.Node;

import java.util.Objects;

public class FirmIrLoop {
	public final Pointer ptr;

	protected FirmIrLoop(Pointer ptr) {
		this.ptr = ptr;
	}

	public static FirmIrLoop createWrapper(Pointer ptr) {
		return new FirmIrLoop(ptr);
	}

	public int depth() {
		return GentleBindings.get_loop_depth(ptr);
	}

	public FirmIrLoop outerLoop() {
		return createWrapper(GentleBindings.get_loop_outer_loop(ptr));
	}

	public boolean isOutermost() {
		return outerLoop().equals(this);
	}

	public int getLoopElementCount() {
		return GentleBindings.get_loop_n_elements(ptr);
	}

	/**
	 * Returns the loop element at the given position. The result is either a {@link FirmIrLoop} or a {@link Node}, but
	 * there is nos shared superclass.
	 *
	 * @param pos the position of the child element. Check against {@link #getLoopElementCount()}
	 *
	 * @return the loop element
	 */
	public Object getElement(int pos) {
		if (pos < 0 || pos >= getLoopElementCount()) {
			throw new IllegalArgumentException(
				"position " + pos + " invalid. Required: 0 <= pos < " + getLoopElementCount());
		}

		Pointer element = GentleBindings.get_loop_element(ptr, pos);
		firm_kind kind = firm_kind.getEnum(binding_firm_common.get_kind(element));

		if (kind == firm_kind.k_ir_loop) {
			return createWrapper(element);
		}
		if (kind == firm_kind.k_ir_node) {
			return Node.createWrapper(element);
		}
		throw new InternalCompilerException("Received unknown loop element " + kind);
	}

	@Override
	public String toString() {
		return binding_irnode.gdb_node_helper(ptr);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		FirmIrLoop that = (FirmIrLoop) o;
		return Objects.equals(ptr, that.ptr);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ptr);
	}
}
