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
		boolean o1 = get().optimizerLevel().orElse(0) >= 1;
		boolean lego = get().lego();
		boolean optimize = o1 || get().optimize();

		boolean constantFolding = !get().noConstantFolding();
		boolean advancedCodeSelection = optimize && !get().noAdvancedCodeSelection();
		boolean arithmeticOpt = optimize && !get().noArithmeticOptimizations();
		boolean booleanOpt = optimize && !get().noBooleanOptimizations();
		boolean escapeAnalysis = optimize && !get().noEscapeAnalysis();
		boolean globalValueNumbering = optimize && !get().noGlobalValueNumbering();
		boolean inlining = optimize && !get().noInlining();
		boolean removePureFunctions = optimize && !get().noRemovePureFunctions();
		boolean removeUnused = optimize && !get().noRemoveUnused();
		boolean removeUnusedGraphs = optimize && !get().noFreeUnusedGraphs();

		return new Optimizations(lego, constantFolding, advancedCodeSelection, arithmeticOpt, booleanOpt,
			escapeAnalysis, globalValueNumbering, inlining, removePureFunctions, removeUnused, removeUnusedGraphs);
	}
}
