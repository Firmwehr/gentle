package com.github.firmwehr.gentle.cli;

public class CompilerArguments {
	private static CommandArguments arguments;

	static void setArguments(CommandArguments arguments) {
		CompilerArguments.arguments = arguments;
	}

	public static CommandArguments get() {
		return arguments;
	}
}
