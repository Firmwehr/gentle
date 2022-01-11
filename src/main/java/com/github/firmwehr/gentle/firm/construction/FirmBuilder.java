package com.github.firmwehr.gentle.firm.construction;

import com.github.firmwehr.gentle.cli.CompilerArguments;
import com.github.firmwehr.gentle.debug.Panopticon;
import com.github.firmwehr.gentle.firm.optimization.ArithmeticOptimization;
import com.github.firmwehr.gentle.firm.optimization.ConstantFolding;
import com.github.firmwehr.gentle.firm.optimization.Optimizer;
import com.github.firmwehr.gentle.firm.optimization.UnusedParameterOptimization;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.semantic.ast.SProgram;
import com.google.common.collect.Lists;
import firm.Backend;
import firm.DebugInfo;
import firm.Firm;
import firm.Graph;
import firm.Program;
import firm.Util;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static com.github.firmwehr.gentle.util.GraphDumper.dumpGraph;
import static java.util.stream.Collectors.joining;

/**
 * Converts a semantic program to a firm graph.
 */
public class FirmBuilder {

	private static final Logger LOGGER = new Logger(FirmBuilder.class);

	static {
		// Must be set before Firm.init is called!
		var maybeVersion = CompilerArguments.get().firmVersion();

		if (maybeVersion.isPresent()) {
			var version = maybeVersion.get();
			LOGGER.info("picked up firm version override to: %s", version);
			Firm.VERSION = maybeVersion.orElse(version);
		} else {
			LOGGER.info("using current firm library default of: %s", Firm.FirmVersion.DEBUG);
			Firm.VERSION = Firm.FirmVersion.DEBUG;
		}
	}

	private final EnumSet<GraphDumpStage> dumpStages;

	public FirmBuilder(GraphDumpStage... stages) {
		this.dumpStages = EnumSet.noneOf(GraphDumpStage.class);
		this.dumpStages.addAll(Arrays.asList(stages));
	}

	/**
	 * Converts a semantic program to a firm graph.
	 *
	 * <p>This method does <em>not</em> call {@link Firm#finish()}.</p>
	 * <p>
	 *
	 * @param program the program to convert
	 * @param panopticon debug and metadata tracker
	 *
	 * @return The generated graphs.
	 *
	 * @throws IOException if writing the assembly file fails
	 */
	public List<Graph> convert(SProgram program, Panopticon panopticon) throws IOException {
		if (!dumpStages.isEmpty()) {
			Backend.option("dump=" + dumpStages.stream().map(GraphDumpStage::getFirmName).collect(joining(",")));
		}
		Firm.init("x86_64-linux-gnu", new String[]{"pic=1"});
		if (Firm.VERSION == Firm.FirmVersion.DEBUG) {
			DebugInfo.init();
		}

		FirmGraphBuilder graphBuilder = new FirmGraphBuilder(panopticon);
		graphBuilder.buildGraph(program);

		// Lower "Member"
		Util.lowerSels();
		for (Graph graph : Program.getGraphs()) {
			dumpGraph(graph, "lower-sel");
		}

		Optimizer.Builder builder = Optimizer.builder();

		if (!CompilerArguments.get().noConstantFolding()) {
			builder.addGraphStep(ConstantFolding.constantFolding());
		}
		if (!CompilerArguments.get().noArithmeticOptimizations()) {
			builder.addGraphStep(ArithmeticOptimization.arithmeticOptimization());
		}
		if (!CompilerArguments.get().noRemoveUnused()) {
			builder.addCallGraphStep(UnusedParameterOptimization.unusedParameterOptimization());
		}

		Optimizer optimizer = builder.build();
		optimizer.optimize();

		return Lists.newArrayList(firm.Program.getGraphs());
	}

	public enum GraphDumpStage {
		DUMP_INITIAL("initial"),
		DUMP_AFTER_SCHEDULING("sched"),
		DUMP_AFTER_PREPARE("prepared"),
		DUMP_AFTER_REGISTER_ALLOCATION("regalloc"),
		DUMP_FINAL_GRAPH("final");

		private final String firmName;

		GraphDumpStage(String firmName) {
			this.firmName = firmName;
		}

		public String getFirmName() {
			return firmName;
		}

		public static GraphDumpStage[] all() {
			return values();
		}
	}
}
