package com.github.firmwehr.gentle;

import com.djdch.log4j.StaticShutdownCallbackRegistry;
import com.github.firmwehr.gentle.cli.CommandArguments;
import com.github.firmwehr.gentle.cli.CommandArgumentsParser;
import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.source.Source;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GentleCompiler {

	// DO NOT MOVE BELOW LOGGER DECLARATION
	static {
		// check for speedcenter run and change logging format
		if (System.getProperty("speedcenter.true") != null) {
			try (var in = GentleCompiler.class.getResourceAsStream("/log4j2_speedcenter.xml")) {
				if (in == null) {
					throw new IOException("Could not find speedcenter logger configuration file");
				}

				var configSource = new ConfigurationSource(in);
				Configurator.initialize(null, configSource);
				//noinspection UseOfSystemOutOrSystemErr
				System.err.println("Detected speedcenter! Switched to speedcenter logger config");
			} catch (IOException e) {

				// can't use logger now since it has not been initialized yet
				//noinspection UseOfSystemOutOrSystemErr
				System.err.println("Detected speedcenter but were unable to load speedcenter configuration");
				System.exit(1);
			}
		}

		System.setProperty("log4j.shutdownCallbackRegistry", "com.djdch.log4j.StaticShutdownCallbackRegistry");
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(GentleCompiler.class);

	public static void main(String[] args) {
		LOGGER.info("Hello World, please be gentle UwU");
		CommandArguments arguments = new CommandArgumentsParser().parseOrExit(args);

		boolean validFlags = arguments.echo() ^ arguments.lextest();
		if (!validFlags) {
			LOGGER.error("Conflicting flags given (none or too many)");
			System.exit(1);
		}

		if (arguments.echo()) {
			echoCommand(arguments.path());
		} else if (arguments.lextest()) {
			lexTestCommand(arguments.path());
		}

		StaticShutdownCallbackRegistry.invoke();
	}

	private static void echoCommand(Path path) {
		try {
			//noinspection UseOfSystemOutOrSystemErr
			System.out.write(Files.readAllBytes(path));
			//noinspection UseOfSystemOutOrSystemErr
			System.out.flush();
		} catch (IOException e) {
			LOGGER.error("Could not echo file '{}': {}", path, e.getMessage());
		}
	}

	private static void lexTestCommand(Path path) {
		try {
			var source = new Source(Files.readString(path, StandardCharsets.UTF_8));
			var lexer = new Lexer(source, Lexer.tokenFilter(TokenType.COMMENT, TokenType.WHITESPACE));

			while (true) {
				var token = lexer.nextToken();
				//noinspection UseOfSystemOutOrSystemErr
				System.out.println(token.format());
				if (token.tokenType() == TokenType.EOF) {
					break;
				}
			}
		} catch (IOException e) {
			LOGGER.error("Could not read for lextest file '{}': {}", path, e.getMessage());
		} catch (LexerException e) {
			LOGGER.error("lextest failed", e);
		}
	}
}
