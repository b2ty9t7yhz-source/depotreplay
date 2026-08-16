package dev.depotreplay.core.path;

import dev.depotreplay.core.model.GridDefinition;
import dev.depotreplay.core.model.PathAlgorithm;
import dev.depotreplay.core.model.Position;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

/** Deterministic four-neighbor Dijkstra and A* search for unit-cost grids. */
public final class GridPathfinder {
    private final GridDefinition grid;
    private final Set<Position> obstacles;

    public GridPathfinder(GridDefinition grid) {
        this.grid = Objects.requireNonNull(grid, "grid");
        this.obstacles = Set.copyOf(grid.obstacles());
    }

    /** Returns path steps excluding start and including goal. */
    public List<Position> findPath(Position start, Position goal, PathAlgorithm algorithm) {
        validateEndpoint(start, "start");
        validateEndpoint(goal, "goal");
        Objects.requireNonNull(algorithm, "algorithm");
        if (start.equals(goal)) {
            return List.of();
        }

        Map<Position, Integer> distance = new HashMap<>();
        Map<Position, Position> previous = new HashMap<>();
        Set<Position> settled = new HashSet<>();
        Comparator<Node> ordering = Comparator.comparingInt(Node::priority)
                .thenComparingInt(Node::distance)
                .thenComparing(Node::position);
        PriorityQueue<Node> frontier = new PriorityQueue<>(ordering);
        distance.put(start, 0);
        frontier.add(new Node(start, 0, heuristic(start, goal, algorithm)));

        while (!frontier.isEmpty()) {
            Node current = frontier.remove();
            if (!settled.add(current.position())) {
                continue;
            }
            if (current.position().equals(goal)) {
                return reconstruct(previous, start, goal);
            }
            for (Position neighbor : neighbors(current.position())) {
                if (settled.contains(neighbor)) {
                    continue;
                }
                int candidate = current.distance() + 1;
                int known = distance.getOrDefault(neighbor, Integer.MAX_VALUE);
                if (candidate < known) {
                    distance.put(neighbor, candidate);
                    previous.put(neighbor, current.position());
                    frontier.add(new Node(
                            neighbor,
                            candidate,
                            candidate + heuristic(neighbor, goal, algorithm)
                    ));
                }
            }
        }
        throw new PathNotFoundException(start, goal);
    }

    public int distance(Position start, Position goal, PathAlgorithm algorithm) {
        return findPath(start, goal, algorithm).size();
    }

    public List<Position> neighbors(Position position) {
        List<Position> candidates = new ArrayList<>(4);
        candidates.add(new Position(position.x() - 1, position.y()));
        candidates.add(new Position(position.x(), position.y() - 1));
        candidates.add(new Position(position.x(), position.y() + 1));
        candidates.add(new Position(position.x() + 1, position.y()));
        return candidates.stream()
                .filter(this::isTraversable)
                .sorted()
                .toList();
    }

    public boolean isTraversable(Position position) {
        return position != null
                && position.x() >= 0 && position.x() < grid.width()
                && position.y() >= 0 && position.y() < grid.height()
                && !obstacles.contains(position);
    }

    private int heuristic(Position position, Position goal, PathAlgorithm algorithm) {
        return algorithm == PathAlgorithm.ASTAR ? position.manhattanDistance(goal) : 0;
    }

    private void validateEndpoint(Position position, String label) {
        if (!isTraversable(position)) {
            throw new IllegalArgumentException(label + " must be traversable: " + position);
        }
    }

    private static List<Position> reconstruct(
            Map<Position, Position> previous,
            Position start,
            Position goal
    ) {
        Deque<Position> reverse = new ArrayDeque<>();
        Position current = goal;
        while (!current.equals(start)) {
            reverse.addFirst(current);
            current = previous.get(current);
            if (current == null) {
                throw new IllegalStateException("Broken predecessor chain");
            }
        }
        return List.copyOf(reverse);
    }

    private record Node(Position position, int distance, int priority) { }
}
