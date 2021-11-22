package com.github.firmwehr.gentle;

import com.github.firmwehr.gentle.cli.CommandArguments;
import com.github.firmwehr.gentle.cli.CommandArgumentsParser;
import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.output.UserOutput;
import com.github.firmwehr.gentle.parser.ParseException;
import com.github.firmwehr.gentle.parser.Parser;
import com.github.firmwehr.gentle.parser.ast.Program;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.parser.tokens.Token;
import com.github.firmwehr.gentle.semantic.SemanticAnalyzer;
import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.source.Source;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class GentleCompiler {

	private static final Logger LOGGER = new Logger(GentleCompiler.class);
	private static final Charset FILE_CHARSET = StandardCharsets.US_ASCII;

	public static void main(String[] args) {
		LOGGER.info("Hello World, please be gentle UwU");
		CommandArguments arguments = new CommandArgumentsParser().parseOrExit(args);

		Set<String> flagsSet = new HashSet<>();
		if (arguments.echo()) {
			flagsSet.add("echo");
		}
		if (arguments.lextest()) {
			flagsSet.add("lextest");
		}
		if (arguments.parsetest()) {
			flagsSet.add("parseTest");
		}
		if (arguments.printAst()) {
			flagsSet.add("printAst");
		}
		if (arguments.check()) {
			flagsSet.add("check");
		}

		try {
			if (flagsSet.isEmpty()) {
				UserOutput.userError("No operation specified.");
				System.exit(1);
			} else if (flagsSet.size() != 1) {
				UserOutput.userError(
					"Conflicting flags set. Received the following mutually exclusive flags: " + flagsSet);
				System.exit(1);
			} else if (arguments.echo()) {
				echoCommand(arguments.path());
			} else if (arguments.lextest()) {
				lexTestCommand(arguments.path());
			} else if (arguments.parsetest()) {
				parseTestCommand(arguments.path());
			} else if (arguments.printAst()) {
				printAstCommand(arguments.path());
			} else if (arguments.check()) {
				checkCommand(arguments.path());
			} else {
				// This can never be reached
				runCommand(arguments.path());
			}
		} catch (Exception e) {
			UserOutput.userMessage("something went wrong, pls annoy me mjtest");
			UserOutput.outputMessage(e.toString());
			//noinspection UseOfSystemOutOrSystemErr
			e.printStackTrace(System.out);
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
				outputStream.writeBytes(token.format().getBytes(FILE_CHARSET));
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
			parser.parse(); // Result ignored
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

	private static void printAstCommand(Path path) {
		try {
			Source source = new Source(Files.readString(path, StandardCharsets.UTF_8));
			Lexer lexer = new Lexer(source, true);
			Parser parser = Parser.fromLexer(source, lexer);
			Program program = parser.parse();
			UserOutput.outputMessage(PrettyPrinter.format(program));
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

	private static void checkCommand(Path path) {
		try {
			Source source = new Source(Files.readString(path, StandardCharsets.UTF_8));
			Lexer lexer = new Lexer(source, true);
			Parser parser = Parser.fromLexer(source, lexer);
			SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(source, parser.parse());
			semanticAnalyzer.analyze(); // Result ignored
		} catch (MalformedInputException e) {
			UserOutput.userError("File contains invalid characters '%s': %s", path, e.getMessage());
			System.exit(1);
		} catch (IOException e) {
			UserOutput.userError("Could not read file '%s': %s", path, e.getMessage());
			System.exit(1);
		} catch (LexerException | ParseException | SemanticException e) {
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
