package com.github.firmwehr.gentle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

import static com.github.firmwehr.gentle.RunUtils.buildRunCommand;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ExtendWith({BuildGentle.class})
@DisabledOnOs(OS.WINDOWS)
public class EchoTest {

	@TempDir
	Path tempFolder;

	@Test
	void testCallDifferentDir() throws IOException {
		Files.writeString(tempFolder.resolve("Foo.txt"), "Hello\nworld!");

		String output = buildRunCommand("--echo", "Foo.txt").withWorkingDirectory(tempFolder.toAbsolutePath().toFile())
			.run()
			.getOutputString();
		assertThat(output).isEqualTo("Hello\nworld!");
	}

	@Test
	void testCallSameDir() throws IOException {
		Files.writeString(tempFolder.resolve("Foo.txt"), "Hello\nworld!");

		Path relativePath = Path.of("").toAbsolutePath().relativize(tempFolder.resolve("Foo.txt").toAbsolutePath());

		String output = buildRunCommand("--echo", relativePath.toString()).run().getOutputString();
		assertThat(output).isEqualTo("Hello\nworld!");
	}

	@Test
	void testCallWithSpace() throws IOException {
		Files.writeString(tempFolder.resolve("Foo hey.txt"), "Hello\nworld!");

		String output =
			buildRunCommand("--echo", "Foo hey.txt").withWorkingDirectory(tempFolder.toFile()).run().getOutputString();
		assertThat(output).isEqualTo("Hello\nworld!");
	}

	@Test
	void testDifferentLineEndings() throws IOException {
		Files.writeString(tempFolder.resolve("Foo hey.txt"), "Hello\r\nworld\nLast line!");

		String output =
			buildRunCommand("--echo", "Foo hey.txt").withWorkingDirectory(tempFolder.toFile()).run().getOutputString();
		assertThat(output).isEqualTo("Hello\r\nworld\nLast line!");
	}

	@Test
	void testRandomBytes() throws IOException {
		byte[] bytes = new byte[1024];
		ThreadLocalRandom.current().nextBytes(bytes);
		Files.write(tempFolder.resolve("Foo hey.txt"), bytes);

		byte[] output =
			buildRunCommand("--echo", "Foo hey.txt").withWorkingDirectory(tempFolder.toFile()).run().getOutputBytes();
		assertThat(output).isEqualTo(bytes);
	}

	@Test
	void testNonExistingFile() {
		assertThatThrownBy(() -> buildRunCommand("--echo", "Foo.txt").withWorkingDirectory(tempFolder.toFile())
			.run()).hasMessageContaining("Foo.txt").hasMessageContaining("exist");
	}

	@Test
	void testPermissionDeniedFile() throws IOException {
		Path filePath = tempFolder.resolve("Foo.txt");
		Files.writeString(filePath, "Hello\nworld!");
		Files.setPosixFilePermissions(filePath, Collections.emptySet());

		assertThatThrownBy(() -> buildRunCommand("--echo", "Foo.txt").withWorkingDirectory(tempFolder.toFile())
			.run()).hasMessageContaining("Foo.txt").hasMessageContaining("readable");
	}

	@Test
	void testDirectoryFile() throws IOException {
		Files.createDirectories(tempFolder.resolve("Foo.txt"));

		assertThatThrownBy(() -> buildRunCommand("--echo", "Foo.txt").withWorkingDirectory(tempFolder.toFile())
			.run()).hasMessageContaining("Foo.txt").hasMessageContaining("file");
	}
}
