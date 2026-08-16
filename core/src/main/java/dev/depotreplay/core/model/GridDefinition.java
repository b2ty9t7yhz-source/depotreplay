package dev.depotreplay.core.model;

import java.util.List;

/** Serializable grid dimensions and blocked cells. */
public record GridDefinition(int width, int height, List<Position> obstacles) {
    public GridDefinition {
        obstacles = obstacles == null ? List.of() : List.copyOf(obstacles);
    }
}
