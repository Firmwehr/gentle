package com.github.firmwehr.gentle;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.codegen.CodeSelection;
import com.github.firmwehr.gentle.backend.ir.visit.DjungelskogVisitor;
import com.github.firmwehr.gentle.backend.ir.visit.MolkiVisitor;
import com.github.firmwehr.gentle.cli.CommandArguments;
import com.github.firmwehr.gentle.cli.CommandDispatcher;
import com.github.firmwehr.gentle.debug.Panopticon;
import com.github.firmwehr.gentle.firm.construction.FirmBuilder;
import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.linking.ExternalLinker;
import com.github.firmwehr.gentle.linking.ExternalLinker.RuntimeAbi;
import com.github.firmwehr.gentle.linking.MolkiProcessor;
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
import firm.Backend;
import firm.Graph;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class GentleCompiler {

	private static final Logger LOGGER = new Logger(GentleCompiler.class);
	private static final Charset FILE_CHARSET = StandardCharsets.US_ASCII;

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
				.command(CommandArguments.COMPILE_MOLKI, CommandArguments::compileMolki,
					p -> compileCommand(p, GentleCompiler::generateWithMolkiBackend))
				.defaultCommand(CommandArguments.COMPILE, CommandArguments::compile,
					p -> compileCommand(p, GentleCompiler::generateWithGentleBackend))
				.dispatch(args);
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
			LOGGER.error("Echo failed", e);
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
			Source source = new Source(Files.readString(path, FILE_CHARSET));
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
			Source source = new Source(Files.readString(path, StandardCharsets.UTF_8));
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
			Source source = new Source(Files.readString(path, StandardCharsets.UTF_8));
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
			Source source = new Source(Files.readString(path, StandardCharsets.UTF_8));
			Lexer lexer = new Lexer(source, true);
			Parser parser = Parser.fromLexer(source, lexer);
			SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(source, parser.parse());
			SProgram program = semanticAnalyzer.analyze();

			List<Graph> graphs = new FirmBuilder().convert(program, new Panopticon(source));

			// generate matching filename for input and call backend handler
			String assemblyFilename = FilenameUtils.removeExtension(path.getFileName().toString()) + ".s";
			Path assemblyFile = path.resolveSibling(assemblyFilename);
			handler.handleGraphs(assemblyFile, graphs);

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

	private static void generateWithGentleBackend(Path assemblyFile, List<Graph> graphs) throws IOException {
		LOGGER.info("handing over to gentle backend...");

		Files.deleteIfExists(assemblyFile);
		for (Graph graph : firm.Program.getGraphs()) {
			CodeSelection codeSelection = new CodeSelection(graph);
			List<IkeaBløck> blocks = codeSelection.convertBlocks();
			DjungelskogVisitor visitor = new DjungelskogVisitor();
			String res = visitor.visit(graph, blocks);
			Files.writeString(assemblyFile, res, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
		}

		new ExternalLinker().link(assemblyFile, RuntimeAbi.CDECL);
	}

	private static void generateWithMolkiBackend(Path assemblyFile, List<Graph> graphs) throws IOException {
		LOGGER.info("handing over to gentle/molki backend...");

		Files.deleteIfExists(assemblyFile);
		for (Graph graph : firm.Program.getGraphs()) {
			CodeSelection codeSelection = new CodeSelection(graph);
			List<IkeaBløck> blocks = codeSelection.convertBlocks();
			MolkiVisitor visitor = new MolkiVisitor();
			String res = visitor.visit(graph, blocks);
			Files.writeString(assemblyFile, res, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
		}

		Path finalAssemblyFile = new MolkiProcessor().molkiAssemble(assemblyFile);
		new ExternalLinker().link(finalAssemblyFile, RuntimeAbi.CDECL);
	}

	private static void generateWithFirmBackend(Path assemblyFile, List<Graph> graphs) throws IOException {
		LOGGER.info("handing over to firm backend...");

		String file = assemblyFile.toString();
		Backend.createAssembler(file, assemblyFile.getFileName().toString());
		// TODO: move to compileCommand, once gentle backend can generate working assembler files
		new ExternalLinker().link(assemblyFile, RuntimeAbi.AMD64_SYSTEMV_ABI);
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
		 *
		 * @throws IOException If the handler encountered an error during an IO error during code generation.
		 */
		void handleGraphs(Path assemblyFile, List<Graph> graphs) throws IOException;
	}
}
