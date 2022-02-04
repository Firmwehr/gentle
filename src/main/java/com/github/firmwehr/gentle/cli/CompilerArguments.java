package com.github.firmwehr.gentle.cli;

public class CompilerArguments {
	private static CommandArguments arguments;

	static void setArguments(CommandArguments arguments) {
		CompilerArguments.arguments = arguments;
	}

	public static CommandArguments get() {
		return arguments;
	}

	public static Optimizations optimizations() {
		CommandArguments args = get();

		boolean o1 = args.optimizerLevel().orElse(0) >= 1;
		boolean lego = /* o1 || */ args.lego();
		boolean optimize = o1 || args.optimize();

		boolean constantFolding = !args.noCf();
		boolean advancedCodeSelection = (optimize || args.acs()) && !args.noAcs();
		boolean arithmeticOpt = (optimize || args.arith()) && !args.noArith();
		boolean booleanOpt = (optimize || args.bool()) && !args.noBool();
		boolean escapeAnalysis = (optimize || args.escape()) && !args.noEscape();
		boolean globalValueNumbering = (optimize || args.gvn()) && !args.noGvn();
		boolean inlining = (optimize || args.inline()) && !args.noInline();
		boolean removeUnused = (optimize || args.rmUnused()) && !args.noRmUnused();
		boolean removePureFunctions = (optimize || args.rmPure()) && !args.noRmPure();
		boolean removeUnusedGraphs = (optimize || args.rmGraphs()) && !args.noRmGraphs();
		boolean tailCallOptimization = (optimize || args.tco()) && !args.noTco();
		boolean loopInvariant = (optimize || args.loopInvariant()) && !args.noLoopInvariant();
		boolean reorderInputs = (optimize || args.reorder()) && !args.noReorder();
		boolean loadStore = (optimize || args.loadStore()) && !args.noLoadStore();

		return new Optimizations(lego, constantFolding, advancedCodeSelection, arithmeticOpt, booleanOpt,
			escapeAnalysis, globalValueNumbering, inlining, removeUnused, removePureFunctions, removeUnusedGraphs,
			tailCallOptimization, loopInvariant, reorderInputs, loadStore);
	}
}
