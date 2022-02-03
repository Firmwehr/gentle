package com.github.firmwehr.gentle.util;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.dump.VcgDumper;
import com.github.firmwehr.gentle.backend.lego.register.ControlFlowGraph;
import com.github.firmwehr.gentle.cli.CommandArguments;
import com.github.firmwehr.gentle.cli.CompilerArguments;
import firm.Dump;
import firm.Graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GraphDumper {

	public static Path dumpPath = Path.of(".");

	/**
	 * Dumps a graph but only if {@link CommandArguments#dumpGraphs()} is set.
	 *
	 * @param graph the graph to dump
	 * @param name the name of the file, appended after {@code [graph name]-[number]-}
	 */
	public static void dumpGraph(Graph graph, String name) {
		if (CompilerArguments.get().dumpGraphs()) {
			Dump.dumpGraph(graph, name);
		}
	}

	public static void dumpGraph(ControlFlowGraph controlFlowGraph, String name) {
		if (!CompilerArguments.get().dumpGraphs()) {
			return;
		}

		LegoGraph legoGraph = controlFlowGraph.getStart().nodes().get(0).graph();
		Graph firmGraph = controlFlowGraph.getStart().origin().getGraph();

		char graphDumpNumber = firmGraph.ptr.getChar(192 /* Not a magic number */);
		firmGraph.ptr.setChar(192 /* Still not a magic number */, (char) (graphDumpNumber + 1));

		String fileName = "%s-%02d-%s.vcg".formatted(firmGraph.getEntity().getLdName(), (int) graphDumpNumber, name);
		String asString = new VcgDumper(controlFlowGraph, legoGraph).dumpGraphAsString();

		try {
			Files.writeString(dumpPath.resolve(fileName), asString);
		} catch (IOException e) {
			throw new InternalCompilerException("Failed to dump backend graph", e);
		}
	}
}
