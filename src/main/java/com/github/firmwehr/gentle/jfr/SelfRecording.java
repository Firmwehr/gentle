package com.github.firmwehr.gentle.jfr;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;

public class SelfRecording {

	private static final Path RECORDING_FILE = Path.of("self-recording.jfr");

	public static void withProfiler(boolean jfr, Runnable runnable) {
		if (!jfr) {
			runnable.run();
		} else {
			runProfiled(runnable);
		}
	}

	private static void runProfiled(Runnable runnable) {
		try (Recording recording = new Recording(Configuration.getConfiguration("default"))) {
			recording.start();
			recording.setDumpOnExit(true);
			recording.setDestination(RECORDING_FILE);
			runnable.run();
		} catch (IOException | ParseException e) {
			throw new RuntimeException(e);
		}
	}
}
