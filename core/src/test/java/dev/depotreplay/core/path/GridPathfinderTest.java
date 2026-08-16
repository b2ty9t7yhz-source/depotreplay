package dev.depotreplay.core.path;

import dev.depotreplay.core.model.GridDefinition;
import dev.depotreplay.core.model.PathAlgorithm;
import dev.depotreplay.core.model.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GridPathfinderTest {
    private final GridPathfinder pathfinder = new GridPathfinder(new GridDefinition(
            5,
            5,
            List.of(new Position(1, 0), new Position(1, 1), new Position(1, 2))
    ));

    @Test
    void dijkstraAndAStarReturnTheSameDeterministicShortestPath() {
        Position start = new Position(0, 0);
        Position goal = new Position(2, 0);
        List<Position> expected = List.of(
                new Position(0, 1),
                new Position(0, 2),
                new Position(0, 3),
                new Position(1, 3),
                new Position(2, 3),
                new Position(2, 2),
                new Position(2, 1),
                new Position(2, 0)
        );

        assertEquals(expected, pathfinder.findPath(start, goal, PathAlgorithm.DIJKSTRA));
        assertEquals(expected, pathfinder.findPath(start, goal, PathAlgorithm.ASTAR));
    }

    @Test
    void rejectsBlockedEndpoints() {
        assertThrows(
                IllegalArgumentException.class,
                () -> pathfinder.findPath(new Position(1, 1), new Position(4, 4), PathAlgorithm.ASTAR)
        );
    }

    @Test
    void reportsDisconnectedGoals() {
        GridPathfinder disconnected = new GridPathfinder(new GridDefinition(
                3,
                3,
                List.of(new Position(1, 0), new Position(1, 1), new Position(1, 2))
        ));
        assertThrows(
                PathNotFoundException.class,
                () -> disconnected.findPath(new Position(0, 0), new Position(2, 0), PathAlgorithm.DIJKSTRA)
        );
    }
}
