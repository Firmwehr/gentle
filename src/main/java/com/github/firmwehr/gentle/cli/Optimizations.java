package com.github.firmwehr.gentle.cli;

public record Optimizations(
	boolean lego,
	boolean constantFolding,
	boolean advancedCodeSelection,
	boolean arithmeticOpt,
	boolean booleanOpt,
	boolean escapeAnalysis,
	boolean globalValueNumbering,
	boolean inlining,
	boolean removeUnused,
	boolean removePureFunctions,
	boolean removeUnusedGraphs,
	boolean tailCallOptimization
) {
}
