package com.github.firmwehr.gentle.cli;

import com.github.firmwehr.gentle.output.UserOutput;
import com.github.firmwehr.gentle.util.Pair;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class CommandDispatcher {
	// TODO: Not a very pretty type. Perhaps this could be its own record.
	private final List<Pair<Function<CommandArguments, Boolean>, Consumer<Path>>> commands;

	public CommandDispatcher() {
		this.commands = new ArrayList<>();
	}

	public CommandDispatcher command(Function<CommandArguments, Boolean> flag, Consumer<Path> command) {
		this.commands.add(new Pair<>(flag, command));
		return this;
	}

	public void dispatch(String[] args) {
		CommandArguments arguments = new CommandArgumentsParser().parseOrExit(args);
		List<Consumer<Path>> commands =
			this.commands.stream().filter(cmd -> cmd.first().apply(arguments)).map(Pair::second).toList();

		if (commands.size() == 0) {
			UserOutput.userError("No operation specified.");
			System.exit(1);
		}

		if (commands.size() > 1) {
			// FIXME: Print conflicting flags.
			UserOutput.userError("Conflicting flags set.");
			System.exit(1);
		}

		commands.get(0).accept(arguments.path());
	}
}
