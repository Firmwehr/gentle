package com.github.firmwehr.gentle.cli;

import net.jbock.Command;
import net.jbock.Option;
import net.jbock.Parameter;
import net.jbock.util.StringConverter;

import java.nio.file.Files;
import java.nio.file.Path;

@Command(name = "gentle", description = "A small MiniJava compiler.", publicParser = true)
public interface CommandArguments {

	@Option(names = "--echo", description = "output the file as is")
	boolean echo();

	@Option(names = "--lextest", description = "print all of the file's tokens")
	boolean lextest();

	@Option(names = "--parsetest", description = "exit with 0 iff the file is syntactically valid")
	boolean parsetest();

	@Option(names = "--print-ast", description = "parse and pretty-print the file")
	boolean printAst();

	@Option(names = "--check", description = "exit with 0 iff the file is semantically valid")
	boolean check();

	@Parameter(index = 0, converter = ExistingFileConverter.class, description = "file to read and operate on",
		paramLabel = "FILE")
	Path path();

	class ExistingFileConverter extends StringConverter<Path> {

		@Override
		protected Path convert(String token) {
			Path path = Path.of(token);

			if (Files.notExists(path)) {
				throw new IllegalArgumentException("The file '%s' does not exist".formatted(path));
			}
			if (!Files.isReadable(path)) {
				throw new IllegalArgumentException("The file '%s' is not readable".formatted(path));
			}

			return path;
		}
	}
}
