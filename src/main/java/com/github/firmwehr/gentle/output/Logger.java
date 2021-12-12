package com.github.firmwehr.gentle.output;

import com.google.common.base.Throwables;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import static org.fusesource.jansi.Ansi.ansi;

public class Logger {

	private static final boolean LOGGER_DISABLED = System.getenv("GENTLE_ENABLE_LOG") == null;

	static {
		if (LOGGER_DISABLED) {
			UserOutput.userMessage("Logging disabled. Enable by setting the 'GENTLE_ENABLE_LOG' environment variable");
		} else if (System.getProperty("os.name").contains("win")) {
			AnsiConsole.systemInstall();
		}
	}

	private final LogLevel level;
	private final String prefix;
	private final int headerLength;

	public Logger(Class<?> clazz) {
		this(clazz, LogLevel.INFO);
	}

	public Logger(Class<?> clazz, LogLevel level) {
		this.level = level;
		this.prefix = clazz.getSimpleName();
		//   [debug]         [prefix]          space
		// 7 chars tag   prefix + 2 brackets  1 space
		this.headerLength = 7 + prefix.length() + 2 + 1;
	}

	public boolean isDebugEnabled() {
		if (LOGGER_DISABLED) {
			return false;
		}
		return level.ordinal() <= LogLevel.DEBUG.ordinal();
	}

	public void debug(String message, Throwable throwable) {
		if (LOGGER_DISABLED) {
			return;
		}
		if (level.ordinal() > LogLevel.DEBUG.ordinal()) {
			return;
		}
		debug(message + "%n%s", Throwables.getStackTraceAsString(throwable));
	}

	/**
	 * Prints an emphasised debug message. This is useful if you want to separate multiple stages in your output, e.g.
	 *
	 * <pre>
	 *     <em>Iterating to fixpoint</em>
	 *     Stuff
	 *     Stuff
	 *     <em>Applying found values</em>
	 *     Stuff
	 *     Stuff
	 * </pre>
	 *
	 * @param message the message to print, optionally with format arguments
	 * @param arguments the format arguments
	 */
	public void debugHeader(String message, Object... arguments) {
		debug(ansi().bold().a(message).boldOff().toString(), arguments);
	}

	public void debug(String message, Object... arguments) {
		if (LOGGER_DISABLED) {
			return;
		}
		if (level.ordinal() > LogLevel.DEBUG.ordinal()) {
			return;
		}
		Ansi text = ansi().fgBrightBlack()
			.a("[debug]")
			.fgCyan()
			.a("[" + prefix + "] ")
			.fgBrightBlack()
			.a(indentLinesExceptFirst(message.formatted(arguments)))
			.reset();

		UserOutput.userMessage(text.toString());
	}

	public void info(String message, Throwable throwable) {
		if (LOGGER_DISABLED) {
			return;
		}
		if (level.ordinal() > LogLevel.INFO.ordinal()) {
			return;
		}
		info(message + "%n%s", Throwables.getStackTraceAsString(throwable));
	}

	public void info(String message, Object... arguments) {
		if (LOGGER_DISABLED) {
			return;
		}
		if (level.ordinal() > LogLevel.INFO.ordinal()) {
			return;
		}
		Ansi text = ansi().fgBrightGreen()
			.a("[info ]")
			.fgCyan()
			.a("[" + prefix + "] ")
			.fgGreen()
			.a(indentLinesExceptFirst(message.formatted(arguments)))
			.reset();

		UserOutput.userMessage(text.toString());
	}

	public void warn(String message, Throwable throwable) {
		if (LOGGER_DISABLED) {
			return;
		}
		if (level.ordinal() > LogLevel.WARNING.ordinal()) {
			return;
		}
		warn(message + "%n%s", Throwables.getStackTraceAsString(throwable));
	}

	public void warn(String message, Object... arguments) {
		if (LOGGER_DISABLED) {
			return;
		}
		if (level.ordinal() > LogLevel.WARNING.ordinal()) {
			return;
		}
		Ansi text = ansi().fgBrightMagenta()
			.a("[warn ]")
			.fgCyan()
			.a("[" + prefix + "] ")
			.fgMagenta()
			.a(indentLinesExceptFirst(message.formatted(arguments)))
			.reset();
		UserOutput.userMessage(text.toString());
	}

	public void error(String message, Throwable throwable) {
		if (LOGGER_DISABLED) {
			return;
		}
		error(message + "%n%s", Throwables.getStackTraceAsString(throwable));
	}

	public void error(String message, Object... arguments) {
		if (LOGGER_DISABLED) {
			return;
		}
		Ansi text = ansi().fgBrightRed()
			.a("[error]")
			.fgCyan()
			.a("[" + prefix + "] ")
			.fgRed()
			.a(indentLinesExceptFirst(message.formatted(arguments)))
			.reset();
		UserOutput.userMessage(text.toString());
	}

	private String indentLinesExceptFirst(String input) {
		return input.indent(headerLength).strip();
	}

	public enum LogLevel {
		DEBUG,
		INFO,
		WARNING,
		ERROR
	}
}
