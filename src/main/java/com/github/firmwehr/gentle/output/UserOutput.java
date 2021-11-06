package com.github.firmwehr.gentle.output;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.fusesource.jansi.Ansi.ansi;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class UserOutput {

	/**
	 * Writes data to the program output stream.
	 *
	 * @param outputStream the data to write
	 *
	 * @throws IOException if an error occurs while writing the data
	 */
	public static void outputData(ByteArrayOutputStream outputStream) throws IOException {
		outputStream.writeTo(System.out);
		System.out.flush();
	}

	/**
	 * Writes data to the program output stream.
	 *
	 * @param data the data to write
	 *
	 * @throws IOException if an error occurs while writing the data
	 */
	public static void outputData(byte[] data) throws IOException {
		System.out.write(data);
		System.out.flush();
	}

	/**
	 * Outputs a message to the program output stream.
	 *
	 * @param message the message to write
	 */
	public static void outputMessage(String message) {
		System.out.println(message);
	}

	/**
	 * Outputs a message addressed to the user (written to the program error stream).
	 *
	 * @param message the message to write
	 */
	public static void userMessage(String message) {
		System.err.println(message);
	}

	/**
	 * Outputs an error message (colored in red) addressed to the user (to the program error stream).
	 *
	 * @param message the message to write
	 * @param formatArgs the format arguments
	 */
	public static void userError(String message, Object... formatArgs) {
		userError(message.formatted(formatArgs));
	}

	/**
	 * Outputs the message of a throwable addressed to the user (to the program error stream).
	 *
	 * @param throwable the throwable to write
	 *
	 * @see #userError(String)
	 */
	public static void userError(Throwable throwable) {
		userError(throwable.getMessage());
	}

	/**
	 * Outputs an error message (colored in red) addressed to the user (to the program error stream).
	 *
	 * @param message the message to write
	 */
	public static void userError(String message) {
		userMessage(ansi().fgBrightRed().a("[error] ").fgRed().a(indentLinesExceptFirst(message)).reset().toString());
	}

	private static String indentLinesExceptFirst(String input) {
		return input.indent("[error] ".length()).stripLeading();
	}
}
