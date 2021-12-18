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
		parseBox(boxCorner);
		for (AsciiBox value : boxMap.values()) {
			System.out.println(value);
		}
	}

	public Optional<AsciiBox> parseBox(Point potentialStart) {
		AsciiConnectionChar connectionChar = AsciiConnectionChar.forChar(grid.get(potentialStart)).orElseThrow();
		Optional<BoundingBox> boundingBox = findBoundingBoxFromHit(connectionChar, potentialStart);
		if (boundingBox.isEmpty()) {
			return Optional.empty();
		}
		Point topLeft = boundingBox.get().topLeft();
		Point bottomRight = boundingBox.get().bottomRight();

		if (boxMap.containsKey(topLeft)) {
			return Optional.of(boxMap.get(topLeft));
		}

		List<String> lines = parseBoxLines(topLeft, bottomRight);
		AsciiBox box = new AsciiBox(lines, new ArrayList<>(), new ArrayList<>(), topLeft, bottomRight);

		boxMap.put(topLeft, box);
		populateBoxConnections(box);

		return Optional.of(box);
	}

	private Optional<BoundingBox> findBoundingBoxFromHit(AsciiConnectionChar connectionChar, Point hit) {
		Optional<Point> topLeft;
		Optional<Point> topRight;
		Optional<Point> botLeft;
		Optional<Point> botRight;

		switch (connectionChar) {
			case T_UP, ARROW_DOWN, CORNER_TOP_LEFT -> {
				topLeft = grid.findInRow(hit, CORNER_TOP_LEFT.getCharacter(), LEFT);
				topRight = grid.findInRow(hit, CORNER_TOP_RIGHT.getCharacter(), RIGHT);
				botLeft = topLeft.flatMap(it -> grid.findInColumn(it, CORNER_BOT_LEFT.getCharacter(), DOWN));
				botRight = topRight.flatMap(it -> grid.findInColumn(it, CORNER_BOT_RIGHT.getCharacter(), DOWN));
			}
			case T_DOWN, ARROW_UP, CORNER_TOP_RIGHT -> {
				botLeft = grid.findInRow(hit, CORNER_BOT_LEFT.getCharacter(), LEFT);
				botRight = grid.findInRow(hit, CORNER_BOT_RIGHT.getCharacter(), RIGHT);
				topLeft = botLeft.flatMap(it -> grid.findInColumn(it, CORNER_TOP_LEFT.getCharacter(), UP));
				topRight = botRight.flatMap(it -> grid.findInColumn(it, CORNER_TOP_RIGHT.getCharacter(), UP));
			}
			case T_LEFT, ARROW_RIGHT, CORNER_BOT_LEFT -> {
				topLeft = grid.findInColumn(hit, CORNER_TOP_LEFT.getCharacter(), UP);
				botLeft = grid.findInColumn(hit, CORNER_BOT_LEFT.getCharacter(), DOWN);
				topRight = topLeft.flatMap(it -> grid.findInRow(it, CORNER_TOP_RIGHT.getCharacter(), RIGHT));
				botRight = botLeft.flatMap(it -> grid.findInRow(it, CORNER_BOT_RIGHT.getCharacter(), RIGHT));
			}
			case T_RIGHT, ARROW_LEFT, CORNER_BOT_RIGHT -> {
				topRight = grid.findInColumn(hit, CORNER_TOP_RIGHT.getCharacter(), UP);
				botRight = grid.findInColumn(hit, CORNER_BOT_RIGHT.getCharacter(), DOWN);
				topLeft = topRight.flatMap(it -> grid.findInRow(it, CORNER_TOP_LEFT.getCharacter(), LEFT));
				botLeft = botRight.flatMap(it -> grid.findInRow(it, CORNER_BOT_LEFT.getCharacter(), LEFT));
			}
			default -> {
				return Optional.empty();
			}
		}

		if (topLeft.isEmpty() || topRight.isEmpty() || botLeft.isEmpty() || botRight.isEmpty()) {
			return Optional.empty();
		}
		if (topLeft.get().x() != botLeft.get().x() || topRight.get().x() != botRight.get().x()) {
			return Optional.empty();
		}
		if (topLeft.get().y() != topRight.get().y() || botLeft.get().y() != botRight.get().y()) {
			return Optional.empty();
		}

		return Optional.of(new BoundingBox(topLeft.get(), botRight.get()));
	}

	private List<String> parseBoxLines(Point topLeft, Point bottomRight) {
		List<String> lines = new ArrayList<>();
		for (int y = topLeft.y() + 1; y < bottomRight.y(); y++) {
			Point lineStart = new Point(topLeft.x() + 1, y);
			String line = grid.readRowUntilCol(lineStart, bottomRight.x());

			lines.add(line.strip());
		}

		if (lines.isEmpty()) {
			throw new IllegalArgumentException("Empty box found at " + topLeft + " -> " + bottomRight);
		}
		return lines;
	}

	private void populateBoxConnections(AsciiBox box) {
		for (Point point : box.edge()) {
			Optional<AsciiConnectionChar> connectionCharOptional = AsciiConnectionChar.forChar(grid.get(point));
			if (connectionCharOptional.isEmpty()) {
				continue;
			}
			AsciiConnectionChar foundChar = connectionCharOptional.get();
			if (!foundChar.isArrow() && !foundChar.isT()) {
				continue;
			}

			boolean isOutgoing = foundChar.isT();
			List<Connection> connections = foundChar.getNeighbours()
				.stream()
				.map(point::translate)
				.filter(not(box::contains))
				.flatMap(pointy -> exploreConnections(pointy, box, isOutgoing).stream())
				.toList();

			if (isOutgoing) {
				box.outs().addAll(connections);
			} else {
				box.ins().addAll(connections);
			}
		}
	}

	public List<Connection> exploreConnections(Point start, AsciiBox source, boolean isOutgoing) {
		List<Connection> connections = new ArrayList<>();

		Set<Point> seen = new HashSet<>();
		Queue<Point> workQueue = new ArrayDeque<>();
		workQueue.add(start);

		while (!workQueue.isEmpty()) {
			Point point = workQueue.poll();
			if (!seen.add(point)) {
				continue;
			}
			if (source.contains(point)) {
				continue;
			}
			if (AsciiConnectionChar.forChar(grid.get(point)).isEmpty()) {
				continue;
			}
			AsciiConnectionChar connectionChar = AsciiConnectionChar.forChar(grid.get(point)).get();

			if (connectionChar.canConnectBoxes()) {
				Optional<AsciiBox> connectedBox = parseBox(point);
				if (connectedBox.isPresent()) {
					AsciiBox box = connectedBox.get();
					if (isOutgoing) {
						connections.add(new Connection(source, start, box, point));
					} else {
						connections.add(new Connection(box, start, source, point));
					}
					continue;
				}
			}

			connectionChar.getNeighbours().stream().map(point::translate).forEach(workQueue::add);
		}

		return connections;
	}

	public static void main(String[] args) {
		new AsciiArt(AsciiGrid.fromString(EXAMPLE)).parse();
	}
}
