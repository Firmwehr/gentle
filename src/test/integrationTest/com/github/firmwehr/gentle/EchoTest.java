package com.github.firmwehr.gentle;

import org.buildobjects.process.ProcBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ExtendWith({BuildGentle.class})
public class EchoTest {
	
	@TempDir
	Path tempFolder;
	
	@Test
	void testCallDifferentDir() throws IOException {
		Files.writeString(tempFolder.resolve("Foo.txt"), "Hello\nworld!");
		String runPath = Path.of("run").toAbsolutePath().toString();
		
		String output = new ProcBuilder("sh", "-c", runPath + " --echo Foo.txt")
				.withWorkingDirectory(tempFolder.toAbsolutePath().toFile())
				.run()
				.getOutputString();
		assertThat(output).isEqualTo("Hello\nworld!\n");
	}
	
	@Test
	void testCallSameDir() throws IOException {
		Files.writeString(tempFolder.resolve("Foo.txt"), "Hello\nworld!");
		String runPath = Path.of("run").toAbsolutePath().toString();
		
		Path relativePath = Path.of("").toAbsolutePath().relativize(tempFolder.resolve("Foo.txt").toAbsolutePath());
		
		String output = new ProcBuilder("sh", "-c", runPath + " --echo " + relativePath)
				.run()
				.getOutputString();
		assertThat(output).isEqualTo("Hello\nworld!\n");
	}
	
	@Test
	void testCallWithSpace() throws IOException {
		Files.writeString(tempFolder.resolve("Foo hey.txt"), "Hello\nworld!");
		String runPath = Path.of("run").toAbsolutePath().toString();
		
		String output = new ProcBuilder("sh", "-c", runPath + " --echo \"Foo hey.txt\"")
				.withWorkingDirectory(tempFolder.toFile())
				.run()
				.getOutputString();
		assertThat(output).isEqualTo("Hello\nworld!\n");
	}
	
	@Test
	void testNonExistingFile() {
		String runPath = Path.of("run").toAbsolutePath().toString();
		
		assertThatThrownBy(() ->
				new ProcBuilder("sh", "-c", runPath + " --echo Foo.txt")
						.withWorkingDirectory(tempFolder.toFile())
						.run()
		)
				.hasMessageContaining("Foo.txt")
				.hasMessageContaining("exist");
	}
	
	@Test
	void testPermissionDeniedFile() throws IOException {
		Path filePath = tempFolder.resolve("Foo.txt");
		Files.writeString(filePath, "Hello\nworld!");
		Files.setPosixFilePermissions(filePath, Collections.emptySet());
		
		String runPath = Path.of("run").toAbsolutePath().toString();
		
		assertThatThrownBy(() ->
				new ProcBuilder("sh", "-c", runPath + " --echo Foo.txt")
						.withWorkingDirectory(tempFolder.toFile())
						.run()
		)
				.hasMessageContaining("Foo.txt")
				.hasMessageContaining("readable");
	}
	
	@Test
	void testDirectoryFile() throws IOException {
		Files.createDirectories(tempFolder.resolve("Foo.txt"));
		String runPath = Path.of("run").toAbsolutePath().toString();
		
		assertThatThrownBy(() ->
				new ProcBuilder("sh", "-c", runPath + " --echo Foo.txt")
						.withWorkingDirectory(tempFolder.toFile())
						.run()
		)
				.hasMessageContaining("Foo.txt")
				.hasMessageContaining("file");
	}
}
