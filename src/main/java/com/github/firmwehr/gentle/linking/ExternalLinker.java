package com.github.firmwehr.gentle.linking;

import com.github.firmwehr.gentle.InternalCompilerException;
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
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		String[] command = {"gcc", assemblyFile, "-g", runtimePath, "-o", outputPath};

		ProcessBuilder processBuilder = new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.DISCARD);

		try {
			Process gccProcess = processBuilder.start();
			gccProcess.getErrorStream().transferTo(byteArrayOutputStream);

			int gccResult = gccProcess.waitFor();
			if (gccResult != 0) {
				throw new InternalCompilerException(
					"gcc execution failed with the following stderr output\n" + byteArrayOutputStream);
			}
		} catch (InterruptedException | IOException e) {
			throw new InternalCompilerException("error while executing gcc. Captured stderr:\n" + byteArrayOutputStream,
				e);
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
