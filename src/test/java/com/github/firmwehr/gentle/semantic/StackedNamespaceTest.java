package com.github.firmwehr.gentle.semantic;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.source.Source;
import com.github.firmwehr.gentle.source.SourceSpan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StackedNamespaceTest {

	private StackedNamespace<Integer> namespace;

	@BeforeEach
	void setUp() {
		this.namespace = new StackedNamespace<>(new Source("Hello world"));
	}

	@Test
	void testLookupSame() throws SemanticException {
		Ident ident = new Ident("Foo", span());
		namespace.put(ident, 1);
		assertThat(namespace.get(ident)).isEqualTo(1);
	}

	@Test
	void testLookupDifferentPosition() throws SemanticException {
		namespace.put(new Ident("Foo", span()), 1);
		assertThat(namespace.get(new Ident("Foo", otherSpan()))).isEqualTo(1);
	}

	@Test
	void testAddSameTwice() throws SemanticException {
		namespace.put(new Ident("Foo", span()), 1);
		assertThatThrownBy(() -> namespace.put(new Ident("Foo", otherSpan()), 2)).isInstanceOf(SemanticException.class);
		assertThat(namespace.get(new Ident("Foo", otherSpan()))).isEqualTo(1);
	}

	@Test
	void testAddSameTwiceDifferentScope() throws SemanticException {
		namespace.put(new Ident("Foo", span()), 1);
		namespace.enterScope();
		assertThatThrownBy(() -> namespace.put(new Ident("Foo", otherSpan()), 2)).isInstanceOf(SemanticException.class);
		assertThat(namespace.get(new Ident("Foo", otherSpan()))).isEqualTo(1);
		namespace.leaveScope();
		assertThat(namespace.get(new Ident("Foo", otherSpan()))).isEqualTo(1);
	}

	@Test
	void testLookupNested() throws SemanticException {
		Ident ident = new Ident("Foo", span());
		namespace.put(ident, 1);

		namespace.enterScope();
		assertThat(namespace.get(ident)).isEqualTo(1);

		namespace.put(new Ident("Hey", span()), 2);
		assertThat(namespace.get(new Ident("Hey", span()))).isEqualTo(2);

		namespace.leaveScope();
		assertThat(namespace.getOpt(new Ident("Hey", span()))).isEmpty();
		assertThat(namespace.get(ident)).isEqualTo(1);
	}

	private SourceSpan span() {
		return new SourceSpan(1, 1);
	}

	private SourceSpan otherSpan() {
		return new SourceSpan(2, 2);
	}
}
