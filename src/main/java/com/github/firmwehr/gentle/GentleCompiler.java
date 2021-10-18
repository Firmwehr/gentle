package com.github.firmwehr.gentle;

import com.djdch.log4j.StaticShutdownCallbackRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GentleCompiler {
	
	static {
		System.setProperty("log4j.shutdownCallbackRegistry", "com.djdch.log4j.StaticShutdownCallbackRegistry");
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GentleCompiler.class);
	
	public static void main(String[] args) {
		LOGGER.info("Hello World, please be gentle UwU");
		
		StaticShutdownCallbackRegistry.invoke();
	}
}
