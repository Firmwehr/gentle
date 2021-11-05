package com.github.firmwehr.gentle;

import com.github.firmwehr.gentle.cli.CommandArguments;
import com.github.firmwehr.gentle.cli.CommandArgumentsParser;
import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.output.UserOutput;
import com.github.firmwehr.gentle.parser.ParseException;
import com.github.firmwehr.gentle.parser.Parser;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.parser.tokens.Token;
import com.github.firmwehr.gentle.source.Source;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GentleCompiler {

	private static final Logger LOGGER = new Logger(GentleCompiler.class);
	private static final Charset FILE_CHARSET = StandardCharsets.US_ASCII;

	public static void main(String[] args) {
		LOGGER.info("Hello World, please be gentle UwU");
		CommandArguments arguments = new CommandArgumentsParser().parseOrExit(args);

		int flagsSet = 0;
		if (arguments.echo()) {
			flagsSet++;
		}
		if (arguments.lextest()) {
			flagsSet++;
		}
		if (arguments.parsetest()) {
			flagsSet++;
		}

		if (flagsSet != 1) {
			LOGGER.error("Conflicting flags");
			System.exit(1);
		} else if (arguments.echo()) {
			echoCommand(arguments.path());
		} else if (arguments.lextest()) {
			lexTestCommand(arguments.path());
		} else if (arguments.parsetest()) {
			parseTestCommand(arguments.path());
		} else {
			runCommand(arguments.path());
		}

		System.exit(0);
	}

	private static void echoCommand(Path path) {
		try {
			UserOutput.outputData(Files.readAllBytes(path));
		} catch (IOException e) {
			UserOutput.userError("Could not echo file '%s': %s", path, e.getMessage());
			System.exit(1);
		}
	}

	private static void lexTestCommand(Path path) {
		try {
			var source = new Source(Files.readString(path, FILE_CHARSET));
			var lexer = new Lexer(source, true);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			for (Token token : lexer.lex()) {
				outputStream.writeBytes(token.format().getBytes(StandardCharsets.UTF_8));
				outputStream.write('\n');
			}

			UserOutput.outputData(outputStream);
		} catch (MalformedInputException e) {
			UserOutput.userError("File contains invalid characters '%s': %s", path, e.getMessage());
			System.exit(1);
		} catch (IOException e) {
			UserOutput.userError("Could not read file '%s': %s", path, e.getMessage());
			System.exit(1);
		} catch (LexerException e) {
			UserOutput.userError(e);
			System.exit(1);
		}
	}

	private static void parseTestCommand(Path path) {
		try {
			Source source = new Source(Files.readString(path, FILE_CHARSET));
			Lexer lexer = new Lexer(source, true);
			Parser parser = Parser.fromLexer(source, lexer);
			parser.parse(); // result ignored for now
		} catch (MalformedInputException e) {
			UserOutput.userError("File contains invalid characters '%s': %s", path, e.getMessage());
			System.exit(1);
		} catch (IOException e) {
			UserOutput.userError("Could not read file '%s': %s", path, e.getMessage());
			System.exit(1);
		} catch (LexerException | ParseException e) {
			UserOutput.userError(e);
			System.exit(1);
		}
	}

	private static void runCommand(Path path) {
		try {
			Source source = new Source(Files.readString(path, FILE_CHARSET));
			Lexer lexer = new Lexer(source, true);
			Parser parser = Parser.fromLexer(source, lexer);
			LOGGER.info("Parse result:\n%s", PrettyPrinter.format(parser.parse()));
		} catch (MalformedInputException e) {
			UserOutput.userError("File contains invalid characters '%s': %s", path, e.getMessage());
			System.exit(1);
		} catch (IOException e) {
			UserOutput.userError("Could not read file '%s': %s", path, e.getMessage());
			System.exit(1);
		} catch (LexerException | ParseException e) {
			UserOutput.userError(e);
			System.exit(1);
		}
	}
}
