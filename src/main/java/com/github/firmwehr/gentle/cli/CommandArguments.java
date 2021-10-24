package com.github.firmwehr.gentle.cli;

import net.jbock.Command;
import net.jbock.Option;
import net.jbock.util.StringConverter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Command(name = "gentle", description = "A small MiniJava compiler.", publicParser = true)
public interface CommandArguments {

	@Option(names = {"--echo"}, converter = ExistingFileConverter.class,
		description = "Reads a file (relative to the executable) and outputs its contents.", paramLabel = "PATH")
	Optional<Path> path();

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
			if (!Files.isRegularFile(path)) {
				throw new IllegalArgumentException("The file '%s' is not a regular file".formatted(path));
			}

			return path;
		}
	}
}
