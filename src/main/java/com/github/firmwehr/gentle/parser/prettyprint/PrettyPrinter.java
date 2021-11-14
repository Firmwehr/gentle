package com.github.firmwehr.gentle.parser.prettyprint;

import java.util.List;

public class PrettyPrinter {
	private final StringBuilder builder;
	private int indentation;
	private boolean atStartOfLine;

	public static <T extends PrettyPrint> String format(T t) {
		return new PrettyPrinter().add(t).format();
	}

	public PrettyPrinter() {
		builder = new StringBuilder();
		indentation = 0;
		atStartOfLine = true;
	}

	public PrettyPrinter newline() {
		builder.append("\n");
		atStartOfLine = true;

		return this;
	}

	public PrettyPrinter indent() {
		indentation++;

		return this;
	}

	public PrettyPrinter unindent() {
		if (indentation > 0) {
			indentation--;
		}

		return this;
	}

	public PrettyPrinter add(String string) {
		if (atStartOfLine) {
			builder.append("\t".repeat(indentation));
			atStartOfLine = false;
		}

		builder.append(string);

		return this;
	}

	public <T extends PrettyPrint> PrettyPrinter add(T t, PrettyPrint.Parentheses parens) {
		t.prettyPrint(this, parens);

		return this;
	}

	public <T extends PrettyPrint> PrettyPrinter add(T t) {
		return add(t, PrettyPrint.Parentheses.INCLUDE);
	}


	public <T extends PrettyPrint> PrettyPrinter addAll(
		List<T> ts, String separator, boolean newlines, PrettyPrint.Parentheses parens
	) {
		if (!ts.isEmpty()) {
			for (int i = 0; i < ts.size() - 1; i++) {
				add(ts.get(i), parens).add(separator);
				if (newlines) {
					newline();
				}
			}

			add(ts.get(ts.size() - 1), parens);
			if (newlines) {
				newline();
			}
		}

		return this;
	}

	public <T extends PrettyPrint> PrettyPrinter addAll(List<T> ts, String separator, boolean newlines) {
		return addAll(ts, separator, newlines, PrettyPrint.Parentheses.OMIT);
	}

	public String format() {
		return builder.toString();
	}
}
