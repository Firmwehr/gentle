package com.github.firmwehr.gentle.asciiart;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import static com.github.firmwehr.gentle.asciiart.AsciiConnectionChar.CORNER_BOT_LEFT;
import static com.github.firmwehr.gentle.asciiart.AsciiConnectionChar.CORNER_BOT_RIGHT;
import static com.github.firmwehr.gentle.asciiart.AsciiConnectionChar.CORNER_TOP_LEFT;
import static com.github.firmwehr.gentle.asciiart.AsciiConnectionChar.CORNER_TOP_RIGHT;
import static com.github.firmwehr.gentle.asciiart.AsciiGrid.HorizontalDirection.LEFT;
import static com.github.firmwehr.gentle.asciiart.AsciiGrid.HorizontalDirection.RIGHT;
import static com.github.firmwehr.gentle.asciiart.AsciiGrid.VerticalDirection.DOWN;
import static com.github.firmwehr.gentle.asciiart.AsciiGrid.VerticalDirection.UP;
import static java.util.function.Predicate.not;

public class AsciiArt {
	private static final String EXAMPLE = """
		           ┌──────────────────┐  ┌────────────────┐   ┌────────────┐
		           │ mem: * ; +memory │  │lhs: * ; -memory│   │rhs: Const 1│
		           └────────────┬─────┘  └───┬────────────┘   └────┬───────┘
		                        │            │                     │
		                        └────────┐   │   ┌─────────────────┘
		                                 │   │   │
		                                ┌▼───▼───▼─┐
		                     ┌──────────┤ div: Div ├──────┐
		                     │          └──────────┘      │
		                     │                            │
		┌────────────────────▼───────┐           ┌────────▼───────────────────┐
		│ memOuts: * ; +memory ; list│           │dataOuts: * ; -memory ; list│
		└────────────────────────────┘           └────────────────────────────┘
		""";

	private final AsciiGrid grid;
	private final Map<Point, AsciiBox> boxMap;

	public AsciiArt(AsciiGrid grid) {
		this.grid = grid;
		this.boxMap = new HashMap<>();
	}

	public void parse() {
		Point boxCorner = grid.find('┌').orElseThrow();
		System.out.println(boxCorner);
		parseBoxy(boxCorner, true);
		for (AsciiBox value : boxMap.values()) {
			System.out.println(value);
		}
	}

	public Optional<AsciiBox> parseBoxy(Point potentialStart) {
		return parseBoxy(potentialStart, false);
	}

	public Optional<AsciiBox> parseBoxy(Point potentialStart, boolean allowCorner) {
		AsciiConnectionChar connectionChar = AsciiConnectionChar.forChar(grid.get(potentialStart)).orElseThrow();
		Point topLeft;
		Point bottomRight;
		switch (connectionChar) {
			case VERTICAL, HORIZONTAL, CORNER_TOP_RIGHT, CORNER_BOT_RIGHT, CORNER_BOT_LEFT -> {
				return Optional.empty();
			}
			case CORNER_TOP_LEFT, ARROW_DOWN, T_UP -> {
				Point topRight = grid.findInRow(potentialStart, CORNER_TOP_RIGHT.getCharacter(), RIGHT);
				topLeft = grid.findInRow(potentialStart, CORNER_TOP_LEFT.getCharacter(), LEFT);
				Point bottomLeft = grid.findInColumn(topLeft, CORNER_BOT_LEFT.getCharacter(), DOWN);
				bottomRight = grid.findInColumn(topRight, CORNER_BOT_RIGHT.getCharacter(), DOWN);

				if (bottomLeft.y() != bottomRight.y()) {
					return Optional.empty();
				}
			}
			case ARROW_UP, T_DOWN -> {
				bottomRight = grid.findInRow(potentialStart, CORNER_BOT_RIGHT.getCharacter(), RIGHT);
				Point bottomLeft = grid.findInRow(potentialStart, CORNER_BOT_LEFT.getCharacter(), LEFT);
				topLeft = grid.findInColumn(bottomLeft, CORNER_TOP_LEFT.getCharacter(), UP);
				Point topRight = grid.findInColumn(bottomRight, CORNER_TOP_RIGHT.getCharacter(), UP);

				if (topLeft.y() != topRight.y()) {
					return Optional.empty();
				}
			}
			case ARROW_LEFT, T_RIGHT -> {
				Point topRight = grid.findInColumn(potentialStart, CORNER_TOP_RIGHT.getCharacter(), UP);
				bottomRight = grid.findInColumn(potentialStart, CORNER_BOT_RIGHT.getCharacter(), DOWN);
				topLeft = grid.findInRow(topRight, CORNER_TOP_LEFT.getCharacter(), LEFT);
				Point bottomLeft = grid.findInRow(bottomRight, CORNER_BOT_LEFT.getCharacter(), LEFT);

				if (topLeft.x() != bottomLeft.x()) {
					return Optional.empty();
				}
			}
			case ARROW_RIGHT, T_LEFT -> {
				topLeft = grid.findInColumn(potentialStart, CORNER_TOP_LEFT.getCharacter(), UP);
				Point bottomLeft = grid.findInColumn(potentialStart, CORNER_BOT_LEFT.getCharacter(), DOWN);
				Point topRight = grid.findInRow(topLeft, CORNER_TOP_RIGHT.getCharacter(), RIGHT);
				bottomRight = grid.findInRow(bottomLeft, CORNER_BOT_RIGHT.getCharacter(), RIGHT);

				if (topRight.x() != bottomRight.x()) {
					return Optional.empty();
				}
			}
			default -> throw new IllegalArgumentException(":(");
		}

		if (boxMap.containsKey(topLeft)) {
			return Optional.of(boxMap.get(topLeft));
		}

		List<String> lines = new ArrayList<>();
		for (int y = topLeft.y() + 1; y < bottomRight.y(); y++) {
			Point lineStart = new Point(topLeft.x() + 1, y);
			String line = grid.readRowUntilCol(lineStart, bottomRight.x());

			lines.add(line.strip());
		}

		if (lines.isEmpty()) {
			throw new IllegalArgumentException("Empty box found at " + topLeft + " -> " + bottomRight);
		}

		AsciiBox box = new AsciiBox(lines, new ArrayList<>(), new ArrayList<>(), topLeft, bottomRight);
		boxMap.put(topLeft, box);

		for (Point point : box.edge()) {
			Optional<AsciiConnectionChar> connectionCharOptional = AsciiConnectionChar.forChar(grid.get(point));
			if (connectionCharOptional.isEmpty()) {
				continue;
			}
			AsciiConnectionChar foundChar = connectionCharOptional.get();
			switch (foundChar) {
				case ARROW_DOWN, ARROW_UP, ARROW_LEFT, ARROW_RIGHT -> {
					List<Connection> connections = foundChar.getNeighbours()
						.stream()
						.map(point::translate)
						.flatMap(pointy -> exploreConnection(pointy, box).stream())
						.toList();
					box.ins().addAll(connections);
				}
				case T_DOWN, T_UP, T_LEFT, T_RIGHT -> {
					List<Connection> connections = foundChar.getNeighbours()
						.stream()
						.map(point::translate)
						.flatMap(pointy -> exploreConnection(pointy, box).stream())
						.toList();
					box.outs().addAll(connections);
				}
			}
		}

		return Optional.of(box);
	}

	public List<Connection> exploreConnection(Point start, AsciiBox source) {
		List<Connection> endpoints = new ArrayList<>();

		Set<Point> seen = new HashSet<>();
		Queue<Point> workQueue = new ArrayDeque<>();
		workQueue.add(start);

		while (!workQueue.isEmpty()) {
			Point point = workQueue.poll();
			if (!seen.add(point)) {
				continue;
			}
			if (source.isInside(point)) {
				continue;
			}
			Optional<AsciiConnectionChar> connectionCharOpt = AsciiConnectionChar.forChar(grid.get(point));

			if (connectionCharOpt.isEmpty()) {
				continue;
			}
			AsciiConnectionChar connectionChar = connectionCharOpt.get();

			if (connectionChar.canConnectBoxes()) {
				Optional<AsciiBox> connectedBox = parseBoxy(point);
				if (connectedBox.isPresent()) {
					AsciiBox box = connectedBox.get();
					endpoints.add(new Connection(box, start, box, point));
					continue;
				}
			}
			connectionChar.getNeighbours()
				.stream()
				.map(point::translate)
				.filter(not(source::isInside))
				.forEach(workQueue::add);
		}

		return endpoints;
	}

	public static void main(String[] args) {
		new AsciiArt(AsciiGrid.fromString(EXAMPLE)).parse();
	}
}
