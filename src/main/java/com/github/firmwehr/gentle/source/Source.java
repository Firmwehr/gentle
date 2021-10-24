package com.github.firmwehr.gentle.source;

import java.util.List;
import java.util.stream.Collectors;

public class Source {
	
	private final String content;
	private final List<String> lines;
	
	public Source(String content) {
		this.content = content;
		this.lines = content.lines().collect(Collectors.toList());
	}
	
	public String formatErrorAtPosition(SourcePosition position, String message, String description) {
		String line;
		if (position.line() < lines.size()) {
			line = lines.get(position.line());
		} else {
			line = "";
		}
		
		StringBuilder builder = new StringBuilder();
		
		builder.append(message)
				.append(" at line ")
				.append(position.format())
				.append("\n#\n# ")
				.append(line)
				.append("\n# ");
		
		line.chars()
				.limit(position.column())
				.map(it -> {
					if (it == ' ' || it == '\t') {
						return it;
					} else {
						return ' ';
					}
				})
				.forEach(it -> builder.append((char) it));
		
		builder.append("^ ")
				.append(description);
		
		return builder.toString();
	}
}
