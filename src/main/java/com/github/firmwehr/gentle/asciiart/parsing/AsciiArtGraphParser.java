package com.github.firmwehr.gentle.asciiart.parsing;

import com.github.firmwehr.gentle.asciiart.elements.AsciiBox;
import com.github.firmwehr.gentle.asciiart.elements.AsciiElement;
import com.github.firmwehr.gentle.asciiart.elements.AsciiMergeNode;
import com.github.firmwehr.gentle.asciiart.util.AsciiConnectionChar;
import com.github.firmwehr.gentle.asciiart.util.AsciiGrid;
import com.github.firmwehr.gentle.asciiart.util.BoundingBox;
import com.github.firmwehr.gentle.asciiart.util.Connection;
import com.github.firmwehr.gentle.asciiart.util.Point;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import static com.github.firmwehr.gentle.asciiart.util.AsciiConnectionChar.CORNER_BOT_LEFT;
import static com.github.firmwehr.gentle.asciiart.util.AsciiConnectionChar.CORNER_BOT_RIGHT;
import static com.github.firmwehr.gentle.asciiart.util.AsciiConnectionChar.CORNER_TOP_LEFT;
import static com.github.firmwehr.gentle.asciiart.util.AsciiConnectionChar.CORNER_TOP_RIGHT;
import static com.github.firmwehr.gentle.asciiart.util.AsciiGrid.HorizontalDirection.LEFT;
import static com.github.firmwehr.gentle.asciiart.util.AsciiGrid.HorizontalDirection.RIGHT;
import static com.github.firmwehr.gentle.asciiart.util.AsciiGrid.VerticalDirection.DOWN;
import static com.github.firmwehr.gentle.asciiart.util.AsciiGrid.VerticalDirection.UP;
import static java.util.function.Predicate.not;

public class AsciiArtGraphParser {
	private static final String EXAMPLE = """
		  ┌─────────────────┐    ┌────────┐
		  │typeSize: Const *│    │index: *│
		  └───────────┬─────┘    └───┬────┘
		              │              │
		              └─────┐   ┌────┘
		                    │   │
		┌────────┐       ┌──▼───▼────┐
		│ base: *│       │offset: Add│
		└────┬───┘       └─────┬─────┘
		     │                 │
		     └───────┬─────────┘
		             │
		      ┌──────▼─────┐
		      │address: Add│
		      └────────────┘""";

	private final AsciiGrid grid;
	private final Map<Point, AsciiBox> boxMap;
	private final Map<Point, AsciiMergeNode> nodeMap;

	public AsciiArtGraphParser(AsciiGrid grid) {
		this.grid = grid;
		this.boxMap = new HashMap<>();
		this.nodeMap = new HashMap<>();
	}

	public AsciiElement parse() {
		Point boxCorner = grid.find('┌').orElseThrow();

		return parseBox(boxCorner).orElseThrow(
			() -> new IllegalArgumentException("Nothing found :/ Do you have a top left corner?"));
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

	public List<Connection> exploreConnections(Point start, AsciiElement source, boolean outgoing) {
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
					if (outgoing && connectionChar.isArrow()) {
						connections.add(new Connection(source, start, box, point));
					} else if (!outgoing && connectionChar.isT()) {
						connections.add(new Connection(box, start, source, point));
					}
					continue;
				} else if (connectionChar.isT()) {
					AsciiMergeNode mergeNode = parseMergeNode(point, connectionChar);
					if (outgoing) {
						connections.add(new Connection(source, start, mergeNode, point));
					} else {
						connections.add(new Connection(mergeNode, point, source, start));
					}
					continue;
				} else {
					throw new IllegalArgumentException("What happened? I got a weird " + connectionChar);
				}
			}

			connectionChar.getNeighbours().stream().map(point::translate).forEach(workQueue::add);
		}

		return connections;
	}

	private AsciiMergeNode parseMergeNode(Point location, AsciiConnectionChar connectionChar) {
		if (nodeMap.containsKey(location)) {
			return nodeMap.get(location);
		}

		AsciiMergeNode mergeNode = new AsciiMergeNode(new ArrayList<>(), location);
		nodeMap.put(location, mergeNode);

		List<Point> nextNodes = connectionChar.getNeighbours().stream().map(location::translate).toList();
		Connection outputConnection =
			nextNodes.stream().flatMap(p -> findMergeNodeEnds(mergeNode, p, true).stream()).findFirst().orElseThrow();
		mergeNode.setOut(outputConnection);

		List<Connection> incoming =
			nextNodes.stream().flatMap(p -> findMergeNodeEnds(mergeNode, p, false).stream()).toList();
		mergeNode.in().addAll(incoming);
		return mergeNode;
	}

	private List<Connection> findMergeNodeEnds(AsciiElement source, Point start, boolean outgoing) {
		Set<Point> seen = new HashSet<>();
		Queue<Point> workQueue = new ArrayDeque<>();
		workQueue.add(start);
		List<Connection> connections = new ArrayList<>();

		while (!workQueue.isEmpty()) {
			Point point = workQueue.poll();
			if (source.contains(point)) {
				continue;
			}
			if (!seen.add(point)) {
				continue;
			}
			if (AsciiConnectionChar.forChar(grid.get(point)).isEmpty()) {
				continue;
			}
			AsciiConnectionChar connectionChar = AsciiConnectionChar.forChar(grid.get(point)).orElseThrow();

			if (connectionChar.isArrow()) {
				if (outgoing) {
					connections.add(new Connection(source, start, parseBox(point).orElseThrow(), point));
					break;
				}
				continue;
			}
			if (connectionChar.isT()) {
				Optional<AsciiBox> box = parseBox(point);
				if (box.isPresent() && !outgoing) {
					connections.add(new Connection(box.get(), point, source, start));
					continue;
				} else if (box.isPresent()) {
					continue;
				}
			}

			connectionChar.getNeighbours().stream().map(point::translate).forEach(workQueue::add);
		}

		return connections;
	}

	public static void main(String[] args) {
		System.out.println(new AsciiArtGraphParser(AsciiGrid.fromString(EXAMPLE)).parse());
	}
}
