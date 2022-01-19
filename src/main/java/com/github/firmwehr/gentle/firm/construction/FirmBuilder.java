package com.github.firmwehr.gentle.firm.construction;

import com.github.firmwehr.gentle.cli.CompilerArguments;
import com.github.firmwehr.gentle.cli.Optimizations;
import com.github.firmwehr.gentle.debug.DebugStore;
import com.github.firmwehr.gentle.firm.optimization.ArithmeticOptimization;
import com.github.firmwehr.gentle.firm.optimization.BooleanOptimization;
import com.github.firmwehr.gentle.firm.optimization.ConstantFolding;
import com.github.firmwehr.gentle.firm.optimization.EscapeAnalysisOptimization;
import com.github.firmwehr.gentle.firm.optimization.FirmGraphCleanup;
import com.github.firmwehr.gentle.firm.optimization.GlobalValueNumbering;
import com.github.firmwehr.gentle.firm.optimization.MethodInliningOptimization;
import com.github.firmwehr.gentle.firm.optimization.Optimizer;
import com.github.firmwehr.gentle.firm.optimization.PureFunctionOptimization;
import com.github.firmwehr.gentle.firm.optimization.TailCallOptimization;
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
	 * @param debugStore debug and metadata tracker
	 *
	 * @return The generated graphs.
	 *
	 * @throws IOException if writing the assembly file fails
	 */
	public List<Graph> convert(SProgram program, DebugStore debugStore) throws IOException {
		if (!dumpStages.isEmpty()) {
			Backend.option("dump=" + dumpStages.stream().map(GraphDumpStage::getFirmName).collect(joining(",")));
		}
		Firm.init("x86_64-linux-gnu", new String[]{"pic=1"});
		if (Firm.VERSION == Firm.FirmVersion.DEBUG) {
			DebugInfo.init();
		}

		FirmGraphBuilder graphBuilder = new FirmGraphBuilder(debugStore);
		graphBuilder.buildGraph(program);

		// Lower "Member"
		Util.lowerSels();
		for (Graph graph : Program.getGraphs()) {
			dumpGraph(graph, "lower-sel");
		}

		Optimizations opts = CompilerArguments.optimizations();
		Optimizer.Builder builder = Optimizer.builder();

		builder.addGraphStep(FirmGraphCleanup.firmGraphCleanup());

		if (opts.constantFolding()) { // constant folding is mandatory to pass course
			builder.addGraphStep(ConstantFolding.constantFolding());
		}

		if (opts.arithmeticOpt()) {
			builder.addGraphStep(ArithmeticOptimization.arithmeticOptimization());
		}
		// firm backend does not know how to deal with Mux, so we can only enable this optimization
		// if we don't use the firm backend
		if (!CompilerArguments.get().compileFirm() && opts.booleanOpt()) {
			builder.addGraphStep(BooleanOptimization.booleanOptimization());
		}
		if (opts.escapeAnalysis()) {
			builder.addGraphStep(EscapeAnalysisOptimization.escapeAnalysisOptimization());
		}
		if (opts.removeUnused()) {
			builder.addCallGraphStep(UnusedParameterOptimization.unusedParameterOptimization());
		}
		if (opts.removePureFunctions()) {
			builder.addCallGraphStep(PureFunctionOptimization.pureFunctionOptimization());
		}
		if (opts.globalValueNumbering()) {
			builder.addGraphStep(GlobalValueNumbering.deduplicate());
		}
		if (opts.inlining()) {
			builder.addCallGraphStep(MethodInliningOptimization.methodInlineOptimization());
		}
		if (opts.removeUnusedGraphs()) {
			builder.freeUnusedGraphs();
		}
		if (opts.tailCallOptimization()) {
			builder.addGraphStep(TailCallOptimization.tailCallOptimization());
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
