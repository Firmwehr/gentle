package com.github.firmwehr.gentle.output;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.fusesource.jansi.Ansi.ansi;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public final class UserOutput {

	private static final UserOutput INSTANCE = new UserOutput();

	private UserOutput() {
	}

	/**
	 * Writes data to the program output stream.
	 *
	 * @param outputStream the data to write
	 *
	 * @throws IOException if an error occurs while writing the data
	 */
	public void outputData(ByteArrayOutputStream outputStream) throws IOException {
		outputStream.writeTo(System.out);
		System.out.flush();
	}

	/**
	 * Outputs a message to the program output stream.
	 *
	 * @param message the message to write
	 */
	public void outputMessage(String message) {
		System.out.println(message);
	}

	/**
	 * Outputs a message addressed to the user (written to the program error stream).
	 *
	 * @param message the message to write
	 */
	public void userMessage(String message) {
		System.err.println(message);
	}

	/**
	 * Outputs an error message (colored in red) addressed to the user (to the program error stream).
	 *
	 * @param message the message to write
	 * @param formatArgs the format arguments
	 */
	public void userError(String message, Object... formatArgs) {
		userError(message.formatted(formatArgs));
	}

	/**
	 * Outputs an error message (colored in red) addressed to the user (to the program error stream).
	 *
	 * @param message the message to write
	 */
	public void userError(String message) {
		userMessage(ansi().fgBrightRed().a("[error] ").fgRed().a(indentLinesExceptFirst(message)).reset().toString());
	}

	private String indentLinesExceptFirst(String input) {
		return input.indent("[error] ".length()).stripLeading();
	}

	public static UserOutput getInstance() {
		return INSTANCE;
	}
}
