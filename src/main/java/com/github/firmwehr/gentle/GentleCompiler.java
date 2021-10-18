package com.github.firmwehr.gentle;

import com.djdch.log4j.StaticShutdownCallbackRegistry;
import com.github.firmwehr.gentle.cli.CommandArguments;
import com.github.firmwehr.gentle.cli.CommandArgumentsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class GentleCompiler {
	
	static {
		System.setProperty("log4j.shutdownCallbackRegistry", "com.djdch.log4j.StaticShutdownCallbackRegistry");
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GentleCompiler.class);
	
	public static void main(String[] args) {
		LOGGER.info("Hello World, please be gentle UwU");
		LOGGER.info("Your args are: {}", (Object) args);
		LOGGER.info("My current working directory is: {}", Path.of(".").toAbsolutePath());
		CommandArguments arguments = new CommandArgumentsParser().parseOrExit(args);
		System.out.println(arguments.path());
		
		StaticShutdownCallbackRegistry.invoke();
	}
}
