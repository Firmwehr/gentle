package com.github.firmwehr.gentle.firm;

import com.github.firmwehr.gentle.semantic.ast.SProgram;
import firm.Backend;
import firm.Construction;
import firm.Dump;
import firm.Entity;
import firm.Firm;
import firm.Graph;
import firm.MethodType;
import firm.Mode;
import firm.Program;
import firm.Type;
import firm.nodes.Node;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Path;

public class FirmBuilder {

	public void convert(Path file, SProgram program) throws IOException {
		System.out.println(Path.of("").toAbsolutePath());
		Firm.init("x86_64-linux-gnu", new String[]{"pic=1"});

		MethodType mainMethod = new MethodType(new Type[]{}, new Type[]{Mode.getIs().getType()});
		Entity mainEntity = new Entity(Program.getGlobalType(), "main", mainMethod);

		Graph mainGraph = new Graph(mainEntity, 0);
		Construction construction = new Construction(mainGraph);

		Node oneConst = construction.newConst(1, Mode.getIs());
		Node returnNode = construction.newReturn(construction.getCurrentMem(), new Node[]{oneConst});
		mainGraph.getEndBlock().addPred(returnNode);

		construction.finish();

		Dump.dumpGraph(mainGraph, "test");

		String basename = FilenameUtils.removeExtension(file.getFileName().toString());
		String assemblerFile = basename + ".s";
		Backend.createAssembler(assemblerFile, assemblerFile);
		Runtime.getRuntime().exec(new String[]{"gcc", assemblerFile, "-o", basename});

		Firm.finish();
	}
}
