package com.github.firmwehr.gentle.linking;

import com.github.firmwehr.gentle.cli.CommandArguments;
import com.github.firmwehr.gentle.cli.CompilerArguments;
import org.buildobjects.process.ProcBuilder;

import java.nio.file.Path;

public class MolkiProcessor {

	/**
	 * Assembles our generated assembly file using molki. Requires the {@link CommandArguments#molkiBinary()} to be
	 * set.
	 *
	 * @param assemblyFile the assembly file to link and assemble
	 *
	 * @return the final assembly file that can be linked against the molki runtime using GCC
	 */
	public Path molkiAssemble(Path assemblyFile) {
		Path molkiBinary = CompilerArguments.get()
			.molkiBinary()
			.orElseThrow(() -> new IllegalArgumentException(CommandArguments.MOLKI_BINARY + " not set!"));

		ProcBuilder.run(pathToString(molkiBinary), "assemble", pathToString(assemblyFile), "-o", "molki");

		return Path.of("molki.s");
	}

	private String pathToString(Path path) {
		return path.toAbsolutePath().toString();
	}
}
