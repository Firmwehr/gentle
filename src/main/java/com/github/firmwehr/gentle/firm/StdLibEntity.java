package com.github.firmwehr.gentle.firm;

import firm.Entity;
import firm.MethodType;
import firm.Mode;
import firm.Program;
import firm.Type;

import java.io.InputStream;
import java.util.function.Supplier;

public enum StdLibEntity {
	/**
	 * @see java.io.PrintStream#flush()
	 */
	FFLUSH(() -> create("fflush", Mode.getP(), Mode.getIs())),
	/**
	 * @see System#out
	 */
	STDOUT(() -> new Entity(Program.getGlobalType(), "stdout", Mode.getP().getType())),
	/**
	 * @see java.io.PrintStream#write(int) System.out.write(int)
	 */
	PUTCHAR(() -> create("putchar", Mode.getIs(), Mode.getIs())),
	/**
	 * @see java.io.PrintStream#println(int) System.out.println
	 */
	PRINTLN(() -> create("println", Mode.getIs(), Mode.getANY())),
	/**
	 * @see InputStream#read() System.in.read()
	 */
	GETCHAR(() -> create("getchar", Mode.getIs())),
	/**
	 * {@code new Object()}
	 */
	MALLOC(() -> create("malloc", Mode.getLu(), Mode.getP())),
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

	private static Entity create(String name, Mode in, Mode out) {
		MethodType methodType = new MethodType(new Type[]{in.getType()}, new Type[]{out.getType()});
		return new Entity(Program.getGlobalType(), name, methodType);
	}
}
