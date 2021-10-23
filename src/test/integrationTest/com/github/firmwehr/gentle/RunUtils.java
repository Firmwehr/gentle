package com.github.firmwehr.gentle;

import org.buildobjects.process.ProcBuilder;

import java.nio.file.Path;

public class RunUtils {
	
	/**
	 * Builds a call to the "run" script.
	 *
	 * @param arguments the arguments to pass to run
	 * @return the configured builder
	 */
	public static ProcBuilder buildRunCommand(String... arguments) {
		String runPath = Path.of("run").toAbsolutePath().toString();
		
		ProcBuilder builder = new ProcBuilder(runPath, arguments);
		
		ProcessHandle.current().info().command().ifPresent(command -> {
			// <root>/bin/java
			Path javaHome = Path.of(command).getParent().getParent();
			builder.withVar("JAVA_HOME", javaHome.toAbsolutePath().toString());
		});
		
		return builder;
	}
}
