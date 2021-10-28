package com.github.firmwehr.gentle.util.codepoints;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StringCodePointIteratorTest {

	@Test
	void handleUnicodeCorrectly() {
		String unicode = "\uD83D\uDCA9";
		int expectedCodePoint = unicode.codePointAt(0);
		assertThat(unicode.length()).isEqualTo(2); // two chars
		var iterator = new StringCodePointIterator(unicode, 0);
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.peekNext()).isEqualTo(expectedCodePoint);
		assertThat(iterator.hasNext()).isTrue(); // peek should not consume
		assertThat(iterator.nextInt()).isEqualTo(expectedCodePoint); // now we advanced the pointer
		assertThat(iterator.hasNext()).isFalse(); // and no more code points should be available
		assertThrows(NoSuchElementException.class, iterator::peekNext);
		assertThrows(NoSuchElementException.class, iterator::nextInt);
	}

	@Test
	void testHandleUnicodeCorrectly2() {
		String text = "Hello \uD83C\uDF7B\uD83E\uDD68\uD83C\uDF7A from Bavaria!";
		int[] asArray =
			{'H', 'e', 'l', 'l', 'o', ' ', 0x1F37B, 0x1F968, 0x1F37A, ' ', 'f', 'r', 'o', 'm', ' ', 'B', 'a', 'v', 'a',
				'r', 'i', 'a', '!'};
		int codePointIndex = 0;
		var iterator = new StringCodePointIterator(text, 0);
		while (iterator.hasNext()) {
			assertThat(iterator.nextInt()).isEqualTo(asArray[codePointIndex++]);
		}
	}

	@Test
	void correctReadCharactersCount() {
		String text = "A\uD83C\uDF7BB";
		var iterator = new StringCodePointIterator(text, 0);
		iterator.nextInt();
		assertThat(iterator.nextIndex()).isEqualTo(1);
		iterator.nextInt();
		assertThat(iterator.nextIndex()).isEqualTo(3);
		iterator.nextInt();
		assertThat(iterator.nextIndex()).isEqualTo(4);
	}

	@Test
	void testInvalidUnicode() {
		String text = "A\uD83CB"; // a single high surrogate char
		System.out.println(text);
		var iterator = new StringCodePointIterator(text, 0);
		iterator.nextInt();
		assertThat(iterator.nextIndex()).isEqualTo(1);
		iterator.nextInt();
		assertThat(iterator.nextIndex()).isEqualTo(2);
		iterator.nextInt();
		assertThat(iterator.nextIndex()).isEqualTo(3);
	}

}
