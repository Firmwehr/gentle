package com.github.firmwehr.gentle.firm;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.ast.SProgram;
import firm.Backend;
import firm.Dump;
import firm.Firm;
import firm.Util;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Path;

public class FirmBuilder {

	public void convert(Path file, SProgram program) throws IOException, SemanticException {
		System.out.println(Path.of("").toAbsolutePath());
		System.in.read();
		//		Backend.option("dump=all");
		Firm.init("x86_64-linux-gnu", new String[]{"pic=1"});

		FirmGraphBuilder graphBuilder = new FirmGraphBuilder();
		graphBuilder.buildGraph(program);

		Util.lowerSels();
		String basename = FilenameUtils.removeExtension(file.getFileName().toString());
		Dump.dumpTypeGraph("types.vcg");
		String assemblerFile = basename + ".s";
		Backend.createAssembler(assemblerFile, assemblerFile);
		Runtime.getRuntime().exec(new String[]{"gcc", assemblerFile, "-g", "runtime.c", "-o", basename});
		Firm.finish();
	}
}
