package dev.depotreplay.core.model;

/** An immutable zero-based grid coordinate. */
public record Position(int x, int y) implements Comparable<Position> {
    public int manhattanDistance(Position other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }

    @Override
    public int compareTo(Position other) {
        int byX = Integer.compare(x, other.x);
        return byX != 0 ? byX : Integer.compare(y, other.y);
    }
}
