package com.github.firmwehr.gentle.cli;

import com.github.firmwehr.gentle.output.UserOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Takes care of running the right command when its corresponding flag was passed as an argument. Also handles some edge
 * cases, e.g. "no command was specified" or "too many commands were specified".
 */
public class CommandDispatcher {
	private final List<CommandSpec> commands;

	public CommandDispatcher() {
		this.commands = new ArrayList<>();
	}

	public CommandDispatcher command(String name, Predicate<CommandArguments> flag, Consumer<Path> command) {
		this.commands.add(new CommandSpec(name, flag, command));
		return this;
	}

	public void dispatch(String[] args) {
		CommandArguments arguments = new CommandArgumentsParser().parseOrExit(args);
		CompilerArguments.setArguments(arguments);

		List<CommandSpec> requestedCommands = this.commands.stream().filter(cmd -> cmd.checkFlag(arguments)).toList();

		if (requestedCommands.isEmpty()) {
			UserOutput.userError("No operation specified.");
			System.exit(1);
		}

		if (requestedCommands.size() > 1) {
			UserOutput.userError("Conflicting flags set. Received the following mutually exclusive flags: " +
				requestedCommands.stream().map(CommandSpec::name).toList());
			System.exit(1);
		}

		requestedCommands.get(0).command().accept(arguments.path());
	}

	private record CommandSpec(
		String name,
		Predicate<CommandArguments> flag,
		Consumer<Path> command
	) {
		public boolean checkFlag(CommandArguments arguments) {
			return flag.test(arguments);
		}
	}
}
