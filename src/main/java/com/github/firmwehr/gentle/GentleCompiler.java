package com.github.firmwehr.gentle;

import com.djdch.log4j.StaticShutdownCallbackRegistry;
import com.github.firmwehr.gentle.cli.CommandArguments;
import com.github.firmwehr.gentle.cli.CommandArgumentsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class GentleCompiler {
	
	static {
		System.setProperty("log4j.shutdownCallbackRegistry", "com.djdch.log4j.StaticShutdownCallbackRegistry");
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GentleCompiler.class);
	
	public static void main(String[] args) {
		LOGGER.info("Hello World, please be gentle UwU");
		CommandArguments arguments = new CommandArgumentsParser().parseOrExit(args);
		
		if (arguments.path().isPresent()) {
			echoCommand(arguments.path().get());
		}
		
		StaticShutdownCallbackRegistry.invoke();
	}
	
	private static void echoCommand(Path path) {
		try (Stream<String> stream = Files.lines(path)) {
			//noinspection UseOfSystemOutOrSystemErr
			stream.forEach(System.out::println);
		} catch (IOException e) {
			LOGGER.error("Could not echo file '{}': {}", path, e.getMessage());
		}
	}
}
