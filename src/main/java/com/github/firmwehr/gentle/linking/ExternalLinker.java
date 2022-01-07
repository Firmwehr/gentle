package com.github.firmwehr.gentle.linking;

import com.github.firmwehr.gentle.InternalCompilerException;
import firm.Firm;
import org.buildobjects.process.ExternalProcessFailureException;
import org.buildobjects.process.ProcBuilder;
import org.buildobjects.process.StartupException;
import org.buildobjects.process.TimeoutException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ExternalLinker {

	/**
	 * Assembles and links a generated assembly file (and compiles the runtime in the process) to an {@code a.out}
	 * file,
	 * placed in the current working directory.
	 *
	 * <p>This method does <em>not</em> call {@link Firm#finish()}.</p>
	 *
	 * @param assemblyFile the assembly file to link and assemble
	 * @param abi the abi of the runtime
	 */
	public void link(Path assemblyFile, RuntimeAbi abi) {
		String runtimePath = extractRuntime(abi).toAbsolutePath().toString();
		// The output should be relative *to our CWD*.
		String outputPath = Path.of("a.out").toAbsolutePath().toString();

		executeGcc(assemblyFile.toAbsolutePath().toString(), runtimePath, outputPath);
	}

	private Path extractRuntime(RuntimeAbi abi) {
		try (InputStream inputStream = getClass().getResourceAsStream(abi.getResource())) {
			if (inputStream == null) {
				throw new InternalCompilerException("could not find runtime in resources folder");
			}

			Path tempFile = Files.createTempFile("gentle-runtime", ".c");
			Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
			tempFile.toFile().deleteOnExit();

			return tempFile;
		} catch (IOException e) {
			throw new InternalCompilerException("runtime extraction failed", e);
		}
	}

	private void executeGcc(String assemblyFile, String runtimePath, String outputPath) {
		try {
			new ProcBuilder("gcc", assemblyFile, "-g", runtimePath, "-o", outputPath).withNoTimeout().run();
		} catch (StartupException | TimeoutException e) {
			throw new InternalCompilerException("Unknown error executing gcc", e);
		} catch (ExternalProcessFailureException e) {
			String message = "Gcc execution failed with exit code " + e.getExitValue() + ", stderr: " + e.getStderr();
			throw new InternalCompilerException(message);
		}
	}

	public enum RuntimeAbi {
		AMD64_SYSTEMV_ABI("/runtime_amd64_systemv.c"),
		CDECL("/runtime_cdecl.c");

		private final String resource;

		RuntimeAbi(String resource) {
			this.resource = resource;
		}

		public String getResource() {
			return resource;
		}
	}
}
