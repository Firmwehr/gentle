package com.github.firmwehr.gentle.cli;

import net.jbock.Command;
import net.jbock.Option;
import net.jbock.Parameter;
import net.jbock.util.StringConverter;

import java.nio.file.Files;
import java.nio.file.Path;

@Command(name = "gentle", description = "A small MiniJava compiler.", publicParser = true)
public interface CommandArguments {

	@Option(names = "--echo", description = "Reads the given file and will output it as it is.")
	boolean echo();

	@Option(names = "--lextest", description = "Reads the given file and prints all tokens or aborts on first error")
	boolean lextest();

	@Option(names = "--parsetest", description = "Reads the given file and exits with 0 iff it is syntactically valid")
	boolean parsetest();

	@Option(names = "--print-ast", description = "Reads the given file and pretty-prints the parser AST")
	boolean printAst();

	@Parameter(index = 0, converter = ExistingFileConverter.class, description = "The file to operate on.",
		paramLabel = "PATH")
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
