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

	@Option(names = "--dump-backend-schedule", description = "include schedule information in backend graphs")
	boolean dumpBackendSchedule();

	@Option(names = "-O",
		description = "set optimization level. -O0 disables all non-required optimizations. -O1 is equivalent to " +
			"--lego --optimize. Defaults to .O0")
	Optional<Integer> optimizerLevel();

	@Option(names = "--lego", description = "use lego + jättestor instead of ikea + djungelskog")
	boolean lego();

	@Option(names = "--optimize", description = "enable all optimizations for the current backend")
	boolean optimize();

	@Option(names = "--acs", description = "enable advanced x86 code generation")
	boolean acs();

	@Option(names = "--no-acs", description = "disable advanced x86 code generation")
	boolean noAcs();

	// Constant folding is always active by default, so there's no point in a --cf flag
	@Option(names = "--no-cf", description = "don't do constant folding")
	boolean noCf();

	@Option(names = "--arith", description = "do arithmetic optimizations")
	boolean arith();

	@Option(names = "--no-arith", description = "don't do arithmetic optimizations")
	boolean noArith();

	@Option(names = "--bool", description = "perform boolean optimizations")
	boolean bool();

	@Option(names = "--no-bool", description = "don't perform boolean optimizations")
	boolean noBool();

	@Option(names = "--escape", description = "remove allocations for objects that do not escape")
	boolean escape();

	@Option(names = "--no-escape", description = "keep allocations for objects that do not escape")
	boolean noEscape();

	@Option(names = "--gvn", description = "enable global value numbering")
	boolean gvn();

	@Option(names = "--no-gvn", description = "disable global value numbering")
	boolean noGvn();

	@Option(names = "--inline", description = "inline code")
	boolean inline();

	@Option(names = "--no-inline", description = "don't inline code")
	boolean noInline();

	@Option(names = "--rm-unused", description = "remove unused call arguments")
	boolean rmUnused();

	@Option(names = "--no-rm-unused", description = "don't remove unused call arguments")
	boolean noRmUnused();

	@Option(names = "--rm-pure", description = "remove pure function calls")
	boolean rmPure();

	@Option(names = "--no-rm-pure", description = "don't remove pure function calls")
	boolean noRmPure();

	@Option(names = "--rm-graphs", description = "remove unreachable graphs")
	boolean rmGraphs();

	@Option(names = "--no-rm-graphs", description = "don't remove unreachable graphs")
	boolean noRmGraphs();

	@Parameter(index = 0, converter = ExistingFileConverter.class, description = "file to read and operate on",
		paramLabel = "FILE")
	Path path();

	@Option(names = "--jfr", description = "run with java flight recorder running")
	boolean jfr();


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
