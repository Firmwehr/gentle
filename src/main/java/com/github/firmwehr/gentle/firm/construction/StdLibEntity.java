package com.github.firmwehr.gentle.firm.construction;

import firm.Entity;
import firm.MethodType;
import firm.Mode;
import firm.Program;
import firm.Type;

import java.io.InputStream;
import java.util.function.Supplier;

/**
 * A firm {@link Entity} for standard functions, mostly implemented by a dedicated runtime.c.
 * <p>The actual entities are created lazily as no firm operations are allowed before calling Firm.init.</p>
 */
public enum StdLibEntity {
	/**
	 * @see java.io.PrintStream#flush() System.out.flush()
	 */
	FLUSH(() -> create("flush", Mode.getANY())),
	/**
	 * @see java.io.PrintStream#write(int) System.out.write(int)
	 */
	PUTCHAR(() -> create("write", Mode.getIs(), Mode.getIs())),
	/**
	 * @see java.io.PrintStream#println(int) System.out.println
	 */
	PRINTLN(() -> create("println", Mode.getANY(), Mode.getIs())),
	/**
	 * @see InputStream#read() System.in.read()
	 */
	GETCHAR(() -> create("getchar", Mode.getIs())),
	/**
	 * {@code new Object()}
	 */
	ALLOCATE(() -> create("allocate", Mode.getP(), Mode.getLu(), Mode.getLu())),
	;

	private final Supplier<Entity> lazyEntityBuilder;

	private Entity entity;

	StdLibEntity(Supplier<Entity> lazyEntityBuilder) {
		this.lazyEntityBuilder = lazyEntityBuilder;
	}

	public Entity getEntity() {
		if (entity == null) {
			entity = lazyEntityBuilder.get();
		}
		return entity;
	}

	private static Entity create(String name, Mode out) {
		MethodType methodType = new MethodType(new Type[]{}, new Type[]{out.getType()});
		return new Entity(Program.getGlobalType(), name, methodType);
	}

	private static Entity create(String name, Mode out, Mode... in) {
		Type[] inputs = new Type[in.length];
		for (int i = 0; i < in.length; i++) {
			inputs[i] = in[i].getType();
		}
		MethodType methodType = new MethodType(inputs, new Type[]{out.getType()});
		return new Entity(Program.getGlobalType(), name, methodType);
	}
}
