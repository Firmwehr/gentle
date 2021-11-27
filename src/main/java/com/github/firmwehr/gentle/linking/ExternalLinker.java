package com.github.firmwehr.gentle.linking;

import firm.Firm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ExternalLinker {

	/**
	 * Assembles and links a generated assembly file (and compiles the runtime in the process) to an {@code a.out}
	 * file,
	 * placed in the same directory as the assembly file.
	 *
	 * <p>This method does <em>not</em> call {@link Firm#finish()}.</p>
	 *
	 * @param assemblyFile the assembly file to link and assemble
	 *
	 * @throws IOException if an external command could not be invoked
	 */
	public void link(Path assemblyFile) throws IOException {
		String runtimePath = extractRuntime().toAbsolutePath().toString();
		String outputPath = assemblyFile.resolveSibling("a.out").toAbsolutePath().toString();

		executeGcc(assemblyFile.toAbsolutePath().toString(), runtimePath, outputPath);
	}

	private Path extractRuntime() throws IOException {
		try (InputStream inputStream = getClass().getResourceAsStream("/runtime.c")) {
			if (inputStream == null) {
				throw new IllegalStateException("Could not find runtime in resources folder");
			}

			Path tempFile = Files.createTempFile("gentle-runtime", ".c");
			Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
			tempFile.toFile().deleteOnExit();

			return tempFile;
		}
	}

	private void executeGcc(String assemblyFile, String runtimePath, String outputPath) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		Process gccProcess =
			new ProcessBuilder("gcc", assemblyFile, "-g", runtimePath, "-o", outputPath).redirectOutput(
				ProcessBuilder.Redirect.DISCARD).start();
		gccProcess.getErrorStream().transferTo(byteArrayOutputStream);

		int gccResult;
		try {
			gccResult = gccProcess.waitFor();
		} catch (InterruptedException e) {
			throw new RuntimeException(e); // TODO: Error handling
		}

		if (gccResult != 0) {
			throw new IOException("Gcc execution failed\n" + byteArrayOutputStream); // TODO: Exception handling
		}
	}
}
