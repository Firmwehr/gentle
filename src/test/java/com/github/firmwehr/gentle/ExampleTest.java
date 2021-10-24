package com.github.firmwehr.gentle;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ExampleTest {

	@Test
	public void testExample() throws IOException {
		var content = IOUtils.toString(getClass().getResource("/example-test.txt"), StandardCharsets.UTF_8);

		Assertions.assertEquals(
			"This is text read from a textfile.",
			content.strip(),
			"Read String does not match expected string"
		);
	}
}
