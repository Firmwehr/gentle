package com.github.firmwehr.gentle.firm.construction;

import com.github.firmwehr.gentle.semantic.ast.SProgram;
import firm.Backend;
import firm.Firm;
import firm.Util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;

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

		FirmGraphBuilder graphBuilder = new FirmGraphBuilder();
		graphBuilder.buildGraph(program);

		// Lower "Member"
		Util.lowerSels();

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