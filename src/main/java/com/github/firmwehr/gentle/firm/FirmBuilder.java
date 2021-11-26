package com.github.firmwehr.gentle.firm;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.ast.SProgram;
import firm.Backend;
import firm.Firm;
import firm.Util;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FirmBuilder {

	static {
		Firm.VERSION = Firm.FirmVersion.DEBUG;
	}

	public void convert(Path file, SProgram program) throws IOException, SemanticException {
		//		System.in.read();
		//		Backend.option("dump=all");
		Firm.init("x86_64-linux-gnu", new String[]{"pic=1"});

		FirmGraphBuilder graphBuilder = new FirmGraphBuilder();
		graphBuilder.buildGraph(program);

		Util.lowerSels();
		String basename = FilenameUtils.removeExtension(file.getFileName().toString());
		String assemblerFile = basename + ".s";
		Backend.createAssembler(assemblerFile, assemblerFile);

		String runtimePath = extractRuntime().toAbsolutePath().toString();
		String outputPath = file.resolveSibling("a.out").toAbsolutePath().toString();

		executeGcc(assemblerFile, runtimePath, outputPath);

		Firm.finish();
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

	private void executeGcc(String assemblerFile, String runtimePath, String outputPath) throws IOException {

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		Process gccProcess =
			new ProcessBuilder("gcc", assemblerFile, "-g", runtimePath, "-o", outputPath).redirectOutput(
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
