package com.github.firmwehr.gentle;

import com.djdch.log4j.StaticShutdownCallbackRegistry;
import com.github.firmwehr.gentle.cli.CommandArguments;
import com.github.firmwehr.gentle.cli.CommandArgumentsParser;
import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.parser.ParseException;
import com.github.firmwehr.gentle.parser.Parser;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.Source;
import com.github.firmwehr.gentle.source.SourceException;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GentleCompiler {

	public static final boolean IS_SPEEDCENTER;

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

			IS_SPEEDCENTER = true;
		} else {
			IS_SPEEDCENTER = false;
		}

		System.setProperty("log4j.shutdownCallbackRegistry", "com.djdch.log4j.StaticShutdownCallbackRegistry");
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(GentleCompiler.class);

	public static void main(String[] args) {
		LOGGER.info("Hello World, please be gentle UwU");
		CommandArguments arguments = new CommandArgumentsParser().parseOrExit(args);

		if (arguments.echo() && arguments.lextest()) {
			LOGGER.error("Conflicting flags");
			System.exit(1);
		} else if (arguments.echo()) {
			echoCommand(arguments.path());
		} else if (arguments.lextest()) {
			lexTestCommand(arguments.path());
		} else {
			runCommand(arguments.path());
		}

		StaticShutdownCallbackRegistry.invoke();
		System.exit(0);
	}

	private static void echoCommand(Path path) {
		try {
			//noinspection UseOfSystemOutOrSystemErr
			System.out.write(Files.readAllBytes(path));
			//noinspection UseOfSystemOutOrSystemErr
			System.out.flush();
		} catch (IOException e) {
			LOGGER.error("Could not echo file '{}': {}", path, e.getMessage());
			System.exit(1);
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
			LOGGER.error("Could not read file '{}': {}", path, e.getMessage());
			System.exit(1);
		} catch (SourceException e) {
			LOGGER.error("Error reading file '{}': {}", path, e.getMessage());
			System.exit(1);
		} catch (LexerException e) {
			LOGGER.error("Lexing failed", e);
			System.exit(1);
		}
	}

	private static void runCommand(Path path) {
		try {
			Source source = new Source(Files.readString(path, StandardCharsets.UTF_8));
			Lexer lexer = new Lexer(source, Lexer.tokenFilter(TokenType.WHITESPACE, TokenType.COMMENT));
			Parser parser = Parser.fromLexer(source, lexer);
			LOGGER.info("Parse result:\n{}", PrettyPrinter.format(parser.parse()));
		} catch (IOException e) {
			LOGGER.error("Could not read file '{}': {}", path, e.getMessage());
			System.exit(1);
		} catch (SourceException e) {
			LOGGER.error("Error reading file '{}': {}", path, e.getMessage());
			System.exit(1);
		} catch (LexerException e) {
			LOGGER.error("Lexing failed", e);
			System.exit(1);
		} catch (ParseException e) {
			LOGGER.error("Parsing failed", e);
			System.exit(1);
		}
	}
}
