package com.github.firmwehr.gentle.cli;

import firm.Firm;
import net.jbock.Command;
import net.jbock.Option;
import net.jbock.Parameter;
import net.jbock.util.StringConverter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@Command(name = "gentle", description = "A small MiniJava compiler.", publicParser = true)
public interface CommandArguments {
	String ECHO = "--echo";
	String LEXTEST = "--lextest";
	String PARSETEST = "--parsetest";
	String PRINT_AST = "--print-ast";
	String CHECK = "--check";
	String COMPILE = "--compile";
	String COMPILE_FIRM = "--compile-firm";
	String FIRM_VERSION = "--firm-version";

	@Option(names = ECHO, description = "output the file as is")
	boolean echo();

	@Option(names = LEXTEST, description = "print all of the file's tokens")
	boolean lextest();

	@Option(names = PARSETEST, description = "exit with 0 iff the file is syntactically valid")
	boolean parsetest();

	@Option(names = PRINT_AST, description = "parse and pretty-print the file")
	boolean printAst();

	@Option(names = CHECK, description = "exit with 0 iff the file is semantically valid")
	boolean check();

	@Option(names = COMPILE, description = "generate a runnable a.out binary using our very good backend")
	boolean compile();

	@Option(names = COMPILE_FIRM, description = "generate a runnable a.out binary using the firm backend")
	boolean compileFirm();

	@Option(names = FIRM_VERSION, description = "firm library version to use", converter = FirmVersionConverter.class)
	Optional<Firm.FirmVersion> firmVersion();

	@Option(names = "--dump-graphs", description = "generate graph dump files")
	boolean dumpGraphs();

	@Option(names = "-O", description = "sets optimization level. 0 will disable all non-required optimization")
	Optional<Integer> optimizerLevel();

	@Option(names = "--no-constant-folding", description = "do not perform constant folding optimization")
	boolean noConstantFolding();

	@Option(names = "--no-arithmetic-optimization", description = "do not perform arithmetic optimizations")
	boolean noArithmeticOptimizations();

	@Option(names = "--no-remove-unused", description = "keep call arguments even if they are not used in the method")
	boolean noRemoveUnused();

	@Option(names = "--no-escape-analysis", description = "keep allocations for objects that do not escape")
	boolean noEscapeAnalysis();

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

	class FirmVersionConverter extends StringConverter<Firm.FirmVersion> {

		// @formatter:off
		private static final Map<String, Firm.FirmVersion> LOOKUP = Map.of(
			"release", Firm.FirmVersion.RELEASE,
			"debug", Firm.FirmVersion.DEBUG,
			"releaseWithDebugSymbols", Firm.FirmVersion.REL_WITH_DEBUG_INFO
		);
		// @formatter:on

		@Override
		protected Firm.FirmVersion convert(String token) {
			var version = LOOKUP.get(token);
			if (version == null) {
				throw new IllegalArgumentException("%s is not a valid library version. (Available: %s)".formatted(token,
					String.join(", ", LOOKUP.keySet())));
			}
			return version;
		}
	}
}
