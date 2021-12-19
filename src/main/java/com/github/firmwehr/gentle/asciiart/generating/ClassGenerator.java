package com.github.firmwehr.gentle.asciiart.generating;

import com.github.firmwehr.gentle.asciiart.elements.AsciiBox;
import com.github.firmwehr.gentle.asciiart.elements.AsciiMergeNode;
import com.github.firmwehr.gentle.lexer.StringReader;
import com.github.firmwehr.gentle.source.Source;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class ClassGenerator {

	private final Map<AsciiBox, FilterElement> elements;
	private final AsciiBox root;

	public ClassGenerator(AsciiBox root) {
		this.root = root;
		this.elements = new HashMap<>();
	}

	public String generate(String name) {
		String result = """
			package com.github.firmwehr.gentle.generated;

			import firm.nodes.*;
			import java.util.*;
			      
			public class Pattern%s {

			""".formatted(name);

		FilterElement rootConverted = convert(root);

		result += buildMatchClass().indent(2).stripTrailing();
		result += "\n";
		result += buildFilter(rootConverted).indent(2).stripTrailing() + "\n";
		result += "\n";
		result += buildMatchMethod().indent(2).stripTrailing() + "\n";
		result += "\n";
		result += "}";

		return result;
	}

	private String buildFilter(FilterElement rootConverted) {
		return "\nprivate final NodeFilter filter = %s;".formatted(rootConverted.filter);
	}

	private String buildMatchClass() {
		String fields = elements.values()
			.stream()
			.sorted(Comparator.comparing(FilterElement::name))
			.map(it -> "private %s %s;".formatted(it.type(), it.name()))
			.collect(Collectors.joining("\n"))
			.indent(2)
			.stripTrailing();

		String methods = elements.values()
			.stream()
			.sorted(Comparator.comparing(FilterElement::name))
			.map(it -> "public %s %s() { return this.%s; }".formatted(it.type(), it.name(), it.name()))
			.collect(Collectors.joining("\n"))
			.indent(2)
			.stripTrailing();

		return "public static class Match {\n%s\n\n%s\n}".formatted(fields, methods);
	}

	private String buildMatchMethod() {
		String assignments = elements.values()
			.stream()
			.sorted(Comparator.comparing(FilterElement::name))
			.map(it -> "match.%s = (%s) matches.get(\"%s\");".formatted(it.name(), it.type(), it.name()))
			.collect(Collectors.joining("\n"))
			.indent(2)
			.strip();

		return """
			public Optional<Match> match(Node node) {
			  if (!filter.matches(node)) {
			    return Optional.empty();
			  }
			  Map<String, Node> matches = new HashMap<>();
			  Match match = new Match();

			  %s

			  return Optional.of(match);
			}
			""".formatted(assignments);
	}

	private FilterElement convert(AsciiBox root) {
		if (elements.containsKey(root)) {
			return elements.get(root);
		}

		StringReader reader = new StringReader(new Source(String.join(" ", root.lines())));
		reader.readWhitespace();
		String name = reader.readWhile(c -> c != ':');
		String quotedName = '"' + name + '"';
		reader.readChar();
		reader.readWhitespace();
		String nodeFilter = reader.readWhile(not(Character::isWhitespace)).strip();
		String nodeType = nodeFilter.equals("*") ? "Node" : nodeFilter;

		String filter = "new ClassFilter(%s, %s)".formatted(quotedName, nodeType + ".class");

		String argument = reader.readWhile(c -> c != ';').strip();
		if (!argument.isEmpty()) {
			filter = switch (nodeType) {
				case "Cmp" -> "new CmpFilter(%s, firm.Relation.%s)".formatted(quotedName, argument);
				case "Const" -> "new ConstFilter(%s, %s)".formatted(quotedName, argument);
				case "Phi" -> "new PhiFilter(%s, %s)".formatted(quotedName,
					argument.equals("+loop") ? "true" : "false");
				case "Proj" -> "new ProjFilter(%s, %s)".formatted(quotedName, argument);
				default -> throw new IllegalArgumentException(
					"This node does not take an argument: " + nodeType + " arg: " + argument);
			};
		}

		if (!root.ins().isEmpty()) {
			List<FilterElement> inFilters;
			String className;
			if (root.ins().get(0).start() instanceof AsciiMergeNode merge) {
				className = "WithInputsUnorderedFilter";
				inFilters = merge.in().stream().map(it -> convert((AsciiBox) it.start())).toList();
			} else {
				className = "WithInputsOrderedFilter";
				inFilters = root.ins().stream().map(it -> convert((AsciiBox) it.start())).toList();
			}

			String inputFilters =
				inFilters.stream().map(FilterElement::filter).collect(Collectors.joining(",\n")).indent(4);
			filter =
				"new %s(\n  %s,\n  %s,\n  List.of(\n%s  )\n)".formatted(className, quotedName, filter, inputFilters);
		}

		FilterElement filterElement = new FilterElement(root, name, filter, nodeType);
		elements.put(root, filterElement);

		return filterElement;
	}

	private record FilterElement(
		AsciiBox element,
		String name,
		String filter,
		String type
	) {
	}
}
