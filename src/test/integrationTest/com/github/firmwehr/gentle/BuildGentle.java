package com.github.firmwehr.gentle;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class BuildGentle implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

	private static boolean started = false;

	@Override
	public void beforeAll(ExtensionContext context) throws IOException, InterruptedException {
		if (!started) {
			//noinspection AssignmentToStaticFieldFromInstanceMethod
			started = true;
			context.getRoot().getStore(GLOBAL).put(getClass().getSimpleName(), this);
			int exitCode = new ProcessBuilder("sh", "-c", "./build").inheritIO().start().waitFor();
			assertThat(exitCode).isZero();
		}
	}

	@Override
	public void close() {
	}
}
