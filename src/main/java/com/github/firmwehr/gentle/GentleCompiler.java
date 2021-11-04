package com.github.firmwehr.gentle;

import com.djdch.log4j.StaticShutdownCallbackRegistry;
import com.github.firmwehr.gentle.cli.CommandArguments;
import com.github.firmwehr.gentle.cli.CommandArgumentsParser;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.lexer2.LexException;
import com.github.firmwehr.gentle.lexer2.Lexer;
import com.github.firmwehr.gentle.parser.ParseException;
import com.github.firmwehr.gentle.parser.Parser;
import com.github.firmwehr.gentle.parser.tokens.CommentToken;
import com.github.firmwehr.gentle.parser.tokens.Token;
import com.github.firmwehr.gentle.parser.tokens.WhitespaceToken;
import com.github.firmwehr.gentle.source.Source;
import com.github.firmwehr.gentle.source.SourceException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GentleCompiler {

	public static void main(String[] args) {
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
			System.exit(1);
		}
	}

	private static void lexTestCommand(Path path) {
		try {
			var source = new Source(Files.readString(path, StandardCharsets.UTF_8));
			var lexer = new Lexer(source);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			for (Token token : lexer.lex()) {
				if (token instanceof CommentToken) {
					continue;
				}
				if (token instanceof WhitespaceToken) {
					continue;
				}
				//noinspection UseOfSystemOutOrSystemErr
				outputStream.writeBytes(token.format().getBytes(StandardCharsets.UTF_8));
				outputStream.write('\n');
			}
			System.out.writeBytes(outputStream.toByteArray());
		} catch (IOException e) {
			System.exit(1);
		} catch (SourceException e) {
			System.exit(1);
		} catch (LexException e) {
			System.exit(1);
		}
	}

	private static void parseTestCommand(Path path) {
		try {
			Source source = new Source(Files.readString(path, StandardCharsets.UTF_8));
			com.github.firmwehr.gentle.lexer.Lexer lexer = new com.github.firmwehr.gentle.lexer.Lexer(source,
				com.github.firmwehr.gentle.lexer.Lexer.tokenFilter(TokenType.WHITESPACE, TokenType.COMMENT));
			Parser parser = Parser.fromLexer(source, lexer);
			parser.parse(); // result ignored for now
		} catch (IOException e) {
			System.exit(1);
		} catch (SourceException e) {
			System.exit(1);
		} catch (LexerException e) {
			System.exit(1);
		} catch (ParseException e) {
			System.exit(1);
		}
	}

	private static void runCommand(Path path) {
		try {
			Source source = new Source(Files.readString(path, StandardCharsets.UTF_8));
			com.github.firmwehr.gentle.lexer.Lexer lexer = new com.github.firmwehr.gentle.lexer.Lexer(source,
				com.github.firmwehr.gentle.lexer.Lexer.tokenFilter(TokenType.WHITESPACE, TokenType.COMMENT));
			Parser parser = Parser.fromLexer(source, lexer);
			System.out.println(parser.parse());
		} catch (IOException e) {
			System.exit(1);
		} catch (SourceException e) {
			System.exit(1);
		} catch (LexerException e) {
			System.exit(1);
		} catch (ParseException e) {
			System.exit(1);
		}
	}
}
