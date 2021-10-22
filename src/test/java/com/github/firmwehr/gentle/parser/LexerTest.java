package com.github.firmwehr.gentle.parser;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LexerTest {
	
	private static Stream<Arguments> provideLexerTestFiles() throws IOException {
		return Files.list(Path.of("src", "test", "resources", "lexer")).filter(path -> path.toString().endsWith(".java")).map(Arguments::of);
	}
	
	@ParameterizedTest
	@MethodSource("provideLexerTestFiles")
	public void lex_shouldNotCrash(Path path) throws IOException {
		BufferedReader input = new BufferedReader(new FileReader(path.toFile()));
		Stream<String> expected = Files.lines(path.resolveSibling(path.getFileName() + ".tokens"));
		Lexer lexer = new Lexer(input);
		
		expected.forEach(expectedToken -> {
			try {
				Token t = lexer.lex();
				assertEquals(expectedToken, t.toString());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
}