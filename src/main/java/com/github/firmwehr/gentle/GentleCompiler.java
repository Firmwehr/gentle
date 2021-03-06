package com.github.firmwehr.gentle;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.visit.DjungelskogVisitor;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.visit.GentleCodegenVisitor;
import com.github.firmwehr.gentle.cli.CommandArguments;
import com.github.firmwehr.gentle.cli.CommandDispatcher;
import com.github.firmwehr.gentle.cli.CompilerArguments;
import com.github.firmwehr.gentle.debug.DebugStore;
import com.github.firmwehr.gentle.firm.FirmJlsFixup;
import com.github.firmwehr.gentle.firm.construction.FirmBuilder;
import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.linking.ExternalLinker;
import com.github.firmwehr.gentle.linking.ExternalLinker.RuntimeAbi;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.output.UserOutput;
import com.github.firmwehr.gentle.parser.ParseException;
import com.github.firmwehr.gentle.parser.Parser;
import com.github.firmwehr.gentle.parser.ast.Program;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.parser.tokens.Token;
import com.github.firmwehr.gentle.semantic.SemanticAnalyzer;
import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.ast.SProgram;
import com.github.firmwehr.gentle.source.Source;
import com.github.firmwehr.gentle.util.GraphDumper;
import firm.Backend;
import firm.Graph;
import firm.bindings.binding_irdump;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class GentleCompiler {

	private static final Logger LOGGER = new Logger(GentleCompiler.class);

	public static void main(String[] args) {
		LOGGER.info("Hello World, please be gentle UwU");

		try {
			new CommandDispatcher().command(CommandArguments.ECHO, CommandArguments::echo, GentleCompiler::echoCommand)
				.command(CommandArguments.LEXTEST, CommandArguments::lextest, GentleCompiler::lexTestCommand)
				.command(CommandArguments.PARSETEST, CommandArguments::parsetest, GentleCompiler::parseTestCommand)
				.command(CommandArguments.PRINT_AST, CommandArguments::printAst, GentleCompiler::printAstCommand)
				.command(CommandArguments.CHECK, CommandArguments::check, GentleCompiler::checkCommand)
				.command(CommandArguments.COMPILE_FIRM, CommandArguments::compileFirm,
					p -> compileCommand(p, GentleCompiler::generateWithFirmBackend))
				.defaultCommand(CommandArguments.COMPILE, CommandArguments::compile,
					p -> compileCommand(p, GentleCompiler::generateWithGentleBackend))
				.dispatch(args);
		} catch (Exception e) {
			UserOutput.userMessage("something went wrong, pls annoy me mjtest");
			UserOutput.outputMessage(e.toString());
			//noinspection UseOfSystemOutOrSystemErr
			e.printStackTrace(System.out);
			System.exit(1);
		}

		System.exit(0);
	}

	private static void echoCommand(Path path) {
		try {
			UserOutput.outputData(Files.readAllBytes(path));
		} catch (IOException e) {
			UserOutput.userError("Could not echo file '%s': %s", path, e.getMessage());
			LOGGER.error("Echo failed", e);
			System.exit(1);
		}
	}

	private static void lexTestCommand(Path path) {
		try {
			var source = Source.loadFromFile(path);
			var lexer = new Lexer(source, true);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			for (Token token : lexer.lex()) {
				outputStream.writeBytes(token.format().getBytes(Source.FILE_CHARSET));
				outputStream.write('\n');
			}

			UserOutput.outputData(outputStream);
		} catch (MalformedInputException e) {
			UserOutput.userError("File contains invalid characters '%s': %s", path, e.getMessage());
			LOGGER.error("Lexing failed", e);
			System.exit(1);
		} catch (IOException e) {
			UserOutput.userError("Could not read file '%s': %s", path, e.getMessage());
			LOGGER.error("Lexing failed", e);
			System.exit(1);
		} catch (LexerException e) {
			UserOutput.userError(e);
			LOGGER.error("Lexing failed", e);
			System.exit(1);
		}
	}

	private static void parseTestCommand(Path path) {
		try {
			Source source = Source.loadFromFile(path);
			Lexer lexer = new Lexer(source, true);
			Parser parser = Parser.fromLexer(source, lexer);
			parser.parse(); // Result ignored
		} catch (MalformedInputException e) {
			UserOutput.userError("File contains invalid characters '%s': %s", path, e.getMessage());
			LOGGER.error("Parsing failed", e);
			System.exit(1);
		} catch (IOException e) {
			UserOutput.userError("Could not read file '%s': %s", path, e.getMessage());
			LOGGER.error("Parsing failed", e);
			System.exit(1);
		} catch (LexerException | ParseException e) {
			UserOutput.userError(e);
			LOGGER.error("Parsing failed", e);
			System.exit(1);
		}
	}

	private static void printAstCommand(Path path) {
		try {
			Source source = Source.loadFromFile(path);
			Lexer lexer = new Lexer(source, true);
			Parser parser = Parser.fromLexer(source, lexer);
			Program program = parser.parse();
			UserOutput.outputMessage(PrettyPrinter.format(program));
		} catch (MalformedInputException e) {
			UserOutput.userError("File contains invalid characters '%s': %s", path, e.getMessage());
			LOGGER.error("AST printing failed", e);
			System.exit(1);
		} catch (IOException e) {
			UserOutput.userError("Could not read file '%s': %s", path, e.getMessage());
			LOGGER.error("AST printing failed", e);
			System.exit(1);
		} catch (LexerException | ParseException e) {
			UserOutput.userError(e);
			LOGGER.error("AST printing failed", e);
			System.exit(1);
		}
	}

	private static void checkCommand(Path path) {
		try {
			Source source = Source.loadFromFile(path);
			Lexer lexer = new Lexer(source, true);
			Parser parser = Parser.fromLexer(source, lexer);
			SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(source, parser.parse());
			semanticAnalyzer.analyze(); // Result ignored
		} catch (MalformedInputException e) {
			UserOutput.userError("File contains invalid characters '%s': %s", path, e.getMessage());
			LOGGER.error("Semantic checking failed", e);
			System.exit(1);
		} catch (IOException e) {
			UserOutput.userError("Could not read file '%s': %s", path, e.getMessage());
			LOGGER.error("Semantic checking failed", e);
			System.exit(1);
		} catch (LexerException | ParseException | SemanticException e) {
			UserOutput.userError(e);
			LOGGER.error("Semantic checking failed", e);
			System.exit(1);
		}
	}

	private static void compileCommand(Path path, CompilerBackendHandler handler) {
		try {
			// trigger load of firm builder class to init firm backend (required by some methods below)
			try {
				Class.forName(FirmBuilder.class.getName());
			} catch (ClassNotFoundException e) {
				throw new InternalCompilerException("firm builder class is missing, what happened?");
			}

			// generate matching filename for input and call backend handler
			String programBaseName = FilenameUtils.removeExtension(path.getFileName().toString());
			String assemblyFilename = programBaseName + ".s";
			Path assemblyFile = path.resolveSibling(assemblyFilename);

			// dump vcg files in directory with program base name, so they are easier to manage
			if (CompilerArguments.get().dumpGraphs()) {
				var dumpBaseDir = path.resolveSibling("vcg/" + programBaseName).toFile();
				FileUtils.forceMkdir(dumpBaseDir);

				binding_irdump.ir_set_dump_path(dumpBaseDir.getPath());
				GraphDumper.dumpPath = dumpBaseDir.toPath();
			}

			Source source = Source.loadFromFile(path);
			Lexer lexer = new Lexer(source, true);
			Parser parser = Parser.fromLexer(source, lexer);
			SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(source, parser.parse());
			SProgram program = semanticAnalyzer.analyze();

			DebugStore debugStore = new DebugStore(source);
			List<Graph> graphs = new FirmBuilder().convert(program, debugStore);
			handler.handleGraphs(assemblyFile, graphs, debugStore);

			System.exit(0);

		} catch (MalformedInputException e) {
			UserOutput.userError("File contains invalid characters '%s': %s", path, e.getMessage());
			LOGGER.error("Compiling using firm failed", e);
			System.exit(1);
		} catch (IOException e) {
			UserOutput.userError("Could not read file '%s': %s", path, e.getMessage());
			LOGGER.error("Compiling using firm failed", e);
			System.exit(1);
		} catch (LexerException | ParseException | SemanticException e) {
			LOGGER.error("Compiling using firm failed", e);
			UserOutput.userError(e);
			System.exit(1);
		}
	}

	private static void generateWithFirmBackend(Path assemblyFile, List<Graph> graphs, DebugStore debugStore)
		throws IOException {
		graphs.forEach(FirmJlsFixup::fix);
		LOGGER.info("handing over to firm backend...");

		String file = assemblyFile.toString();
		Backend.createAssembler(file, assemblyFile.getFileName().toString());
		new ExternalLinker().link(assemblyFile, RuntimeAbi.AMD64_SYSTEMV_ABI);
	}

	private static void generateWithGentleBackend(Path assemblyFile, List<Graph> graphs, DebugStore debugStore)
		throws IOException {
		if (CompilerArguments.optimizations().lego()) {
			generateWithLegoBackend(assemblyFile, graphs, debugStore);
		} else {
			generateWithIkeaBackend(assemblyFile, graphs, debugStore);
		}
	}

	private static void generateWithIkeaBackend(Path assemblyFile, List<Graph> graphs, DebugStore debugStore)
		throws IOException {
		LOGGER.info("handing over to ikea backend...");

		Files.deleteIfExists(assemblyFile);

		int preselectionCount = 0;
		for (Graph graph : firm.Program.getGraphs()) {

			com.github.firmwehr.gentle.backend.ir.codegen.CodePreselection codePreselection;
			if (CompilerArguments.optimizations().advancedCodeSelection()) {
				codePreselection = new com.github.firmwehr.gentle.backend.ir.codegen.CodePreselectionMatcher(graph);
			} else {
				codePreselection = com.github.firmwehr.gentle.backend.ir.codegen.CodePreselection.DUMMY;
			}

			preselectionCount += codePreselection.replacedSubtrees();

			var codeSelection =
				new com.github.firmwehr.gentle.backend.ir.codegen.CodeSelection(graph, codePreselection);
			List<IkeaBløck> blocks = codeSelection.convertBlocks();
			DjungelskogVisitor visitor = new DjungelskogVisitor(debugStore);
			String res = visitor.visit(graph, blocks);
			Files.writeString(assemblyFile, res, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
		}

		LOGGER.info("code preselection matched %s subtrees in total across all graphs", preselectionCount);

		new ExternalLinker().link(assemblyFile, RuntimeAbi.CDECL);
	}

	private static void generateWithLegoBackend(Path assemblyFile, List<Graph> graphs, DebugStore debugStore)
		throws IOException {
		LOGGER.info("handing over to lego backend...");

		Files.deleteIfExists(assemblyFile);

		int preselectionCount = 0;
		for (Graph graph : firm.Program.getGraphs()) {

			com.github.firmwehr.gentle.backend.lego.codegen.CodePreselection codePreselection;
			if (CompilerArguments.optimizations().advancedCodeSelection()) {
				codePreselection = new com.github.firmwehr.gentle.backend.lego.codegen.CodePreselectionMatcher(graph);
			} else {
				codePreselection = com.github.firmwehr.gentle.backend.lego.codegen.CodePreselection.DUMMY;
			}

			preselectionCount += codePreselection.replacedSubtrees();

			var codeSelection =
				new com.github.firmwehr.gentle.backend.lego.codegen.CodeSelection(graph, codePreselection);
			List<LegoPlate> blocks = codeSelection.convertBlocks();

			GentleCodegenVisitor visitor = new GentleCodegenVisitor();
			String res = visitor.visit(graph, blocks);
			Files.writeString(assemblyFile, res, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
		}

		LOGGER.info("code preselection matched %s subtrees in total across all graphs", preselectionCount);

		new ExternalLinker().link(assemblyFile, RuntimeAbi.CDECL);
	}

	/**
	 * Allows registering multiple backends after graph generation.
	 */
	private interface CompilerBackendHandler {

		/**
		 * Called with generated graph after firm phase.
		 *
		 * @param assemblyFile Target name of assembly file.
		 * @param graphs The generated graph.
		 * @param debugStore the generated debug store
		 *
		 * @throws IOException If the handler encountered an error during an IO error during code generation.
		 */
		void handleGraphs(Path assemblyFile, List<Graph> graphs, DebugStore debugStore) throws IOException;
	}
}
