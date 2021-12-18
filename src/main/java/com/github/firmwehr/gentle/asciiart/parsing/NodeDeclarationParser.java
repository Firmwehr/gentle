package com.github.firmwehr.gentle.asciiart.parsing;

import com.github.firmwehr.gentle.asciiart.parsing.filter.NodeFilter;
import com.github.firmwehr.gentle.lexer.StringReader;
import com.github.firmwehr.gentle.source.Source;
import firm.nodes.Node;

public class NodeDeclarationParser {

	public void parse(String input) {
		StringReader reader = new StringReader(new Source(input));

		String name = reader.readWhile(c -> c != ':').strip();
		reader.readChar();

		NodeFilter<Node> filter = new NodeFilterParser().parseFilter(reader);
	}
}
