package com.github.firmwehr.gentle.util;

import com.github.firmwehr.gentle.cli.CommandArguments;
import com.github.firmwehr.gentle.cli.CompilerArguments;
import firm.Dump;
import firm.Graph;

public class GraphDumper {

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
}
