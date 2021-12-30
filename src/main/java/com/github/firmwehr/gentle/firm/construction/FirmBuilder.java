package com.github.firmwehr.gentle.firm.construction;

import com.github.firmwehr.gentle.cli.CompilerArguments;
import com.github.firmwehr.gentle.firm.optimization.UnusedParameterOptimization;
import com.github.firmwehr.gentle.firm.optimization.callgraph.CallGraph;
import com.github.firmwehr.gentle.firm.optimization.ArithmeticOptimization;
import com.github.firmwehr.gentle.firm.optimization.ConstantFolding;
import com.github.firmwehr.gentle.firm.optimization.Optimizer;
import com.github.firmwehr.gentle.semantic.ast.SProgram;
import firm.Backend;
import firm.DebugInfo;
import firm.Firm;
import firm.Graph;
import firm.Program;
import firm.Util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;

import static com.github.firmwehr.gentle.util.GraphDumper.dumpGraph;
import static java.util.stream.Collectors.joining;

/**
 * Converts a semantic program to a firm graph.
 */
public class FirmBuilder {

	static {
		// Must be set before Firm.init is called!
		Firm.VERSION = Firm.FirmVersion.DEBUG;
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
	 *
	 * @param assemblyOutputFile the file to write the assembly code to
	 * @param program the program to convert
	 *
	 * @throws IOException if writing the assembly file fails
	 */
	public void convert(Path assemblyOutputFile, SProgram program) throws IOException {
		if (!dumpStages.isEmpty()) {
			Backend.option("dump=" + dumpStages.stream().map(GraphDumpStage::getFirmName).collect(joining(",")));
		}
		Firm.init("x86_64-linux-gnu", new String[]{"pic=1"});
		if (Firm.VERSION == Firm.FirmVersion.DEBUG) {
			DebugInfo.init();
		}

		FirmGraphBuilder graphBuilder = new FirmGraphBuilder();
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
		if (!CompilerArguments.get().keepUnusedArguments()) {
			builder.addCallGraphStep(UnusedParameterOptimization.unusedParameterOptimization());
		}

		Optimizer optimizer = builder.build();
		optimizer.optimize();

		String assemblyFile = assemblyOutputFile.toAbsolutePath().toString();
		Backend.createAssembler(assemblyFile, assemblyFile);
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
