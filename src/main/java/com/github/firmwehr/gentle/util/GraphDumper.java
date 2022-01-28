package com.github.firmwehr.gentle.util;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.dump.VcgDumper;
import com.github.firmwehr.gentle.backend.ir.register.ControlFlowGraph;
import com.github.firmwehr.gentle.cli.CommandArguments;
import com.github.firmwehr.gentle.cli.CompilerArguments;
import firm.Dump;
import firm.Graph;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

		IkeaGraph ikeaGraph = controlFlowGraph.getStart().nodes().get(0).graph();
		Graph firmGraph = controlFlowGraph.getStart().origin().getGraph();


		Pattern graphFilePattern =
			Pattern.compile(".+" + Pattern.quote(firmGraph.getEntity().getLdName()) + "-(\\d+)-.+\\.vcg");

		try (Stream<Path> files = Files.list(dumpPath)) {
			int maxNum = files.map(Path::toString)
				.filter(graphFilePattern.asMatchPredicate())
				.mapToInt(graphNumber(graphFilePattern))
				.max()
				.orElse(0);


			String fileName = "%s-%02d-%s.vcg".formatted(firmGraph.getEntity().getLdName(), maxNum + 1, name);

			String asString = new VcgDumper(controlFlowGraph, ikeaGraph).dumpGraphAsString();
			Files.writeString(dumpPath.resolve(fileName), asString);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	private static ToIntFunction<String> graphNumber(Pattern graphFilePattern) {
		return it -> {
			Matcher matcher = graphFilePattern.matcher(it);
			if (!matcher.find()) {
				throw new InternalCompilerException("Could not extract dump path " + it);
			}
			return Integer.parseInt(matcher.group(1));
		};
	}
}
