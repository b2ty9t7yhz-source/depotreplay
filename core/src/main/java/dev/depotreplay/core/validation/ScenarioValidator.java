package dev.depotreplay.core.validation;

import dev.depotreplay.core.model.DeliveryTaskDefinition;
import dev.depotreplay.core.model.GridDefinition;
import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ScoreWeights;
import dev.depotreplay.core.model.VehicleDefinition;
import dev.depotreplay.core.model.PathAlgorithm;
import dev.depotreplay.core.path.GridPathfinder;
import dev.depotreplay.core.path.PathNotFoundException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class ScenarioValidator {
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private ScenarioValidator() { }

    public static void validate(Scenario scenario) {
        List<String> violations = new ArrayList<>();
        if (scenario == null) {
            throw new ValidationException(List.of("scenario must not be null"));
        }
        if (scenario.schemaVersion() != 1) {
            violations.add("schemaVersion must be 1");
        }
        if (scenario.name() == null || scenario.name().isBlank() || scenario.name().length() > 80) {
            violations.add("name must contain 1 to 80 visible characters");
        }
        if (scenario.maxTicks() < 1 || scenario.maxTicks() > 10_000) {
            violations.add("maxTicks must be between 1 and 10000");
        }
        if (scenario.pathAlgorithm() == null) {
            violations.add("pathAlgorithm is required");
        }

        GridDefinition grid = scenario.grid();
        Set<Position> obstacles = new HashSet<>();
        if (grid == null) {
            violations.add("grid is required");
        } else {
            if (grid.width() < 2 || grid.width() > 50 || grid.height() < 2 || grid.height() > 50) {
                violations.add("grid width and height must each be between 2 and 50");
            }
            for (Position obstacle : grid.obstacles()) {
                if (obstacle == null || !inBounds(grid, obstacle)) {
                    violations.add("every obstacle must be a non-null in-bounds position");
                } else if (!obstacles.add(obstacle)) {
                    violations.add("obstacle positions must be unique");
                }
            }
        }

        if (scenario.vehicles().size() != 2) {
            violations.add("exactly two vehicles are required");
        }
        Set<String> vehicleIds = new HashSet<>();
        int maximumCapacity = 0;
        for (VehicleDefinition vehicle : scenario.vehicles()) {
            if (vehicle == null) {
                violations.add("vehicle entries must not be null");
                continue;
            }
            validateId("vehicle", vehicle.id(), vehicleIds, violations);
            if (vehicle.capacity() < 1 || vehicle.capacity() > 100) {
                violations.add("vehicle capacity must be between 1 and 100: " + vehicle.id());
            }
            maximumCapacity = Math.max(maximumCapacity, vehicle.capacity());
            validateTraversable("vehicle start", vehicle.start(), grid, obstacles, violations);
        }

        if (scenario.tasks().isEmpty() || scenario.tasks().size() > 100) {
            violations.add("tasks must contain between 1 and 100 entries");
        }
        Set<String> taskIds = new HashSet<>();
        for (DeliveryTaskDefinition task : scenario.tasks()) {
            if (task == null) {
                violations.add("task entries must not be null");
                continue;
            }
            validateId("task", task.id(), taskIds, violations);
            validateTraversable("task pickup", task.pickup(), grid, obstacles, violations);
            validateTraversable("task delivery", task.delivery(), grid, obstacles, violations);
            if (task.pickup() != null && task.pickup().equals(task.delivery())) {
                violations.add("task pickup and delivery must differ: " + task.id());
            }
            if (task.demand() < 1 || task.demand() > maximumCapacity) {
                violations.add("task demand must fit at least one vehicle: " + task.id());
            }
            if (task.deadline() < 1 || task.deadline() > scenario.maxTicks()) {
                violations.add("task deadline must be between 1 and maxTicks: " + task.id());
            }
        }

        validateWeights(scenario.scoreWeights(), violations);
        if (violations.isEmpty()) {
            validateConnectivity(scenario, violations);
        }
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }
    }

    private static void validateId(String kind, String id, Set<String> ids, List<String> violations) {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            violations.add(kind + " id must match " + ID_PATTERN.pattern());
        } else if (!ids.add(id)) {
            violations.add(kind + " ids must be unique: " + id);
        }
    }

    private static void validateTraversable(
            String label,
            Position position,
            GridDefinition grid,
            Set<Position> obstacles,
            List<String> violations
    ) {
        if (position == null || grid == null || !inBounds(grid, position) || obstacles.contains(position)) {
            violations.add(label + " must be a non-null, in-bounds, traversable position");
        }
    }

    private static boolean inBounds(GridDefinition grid, Position position) {
        return position.x() >= 0 && position.x() < grid.width()
                && position.y() >= 0 && position.y() < grid.height();
    }

    private static void validateWeights(ScoreWeights weights, List<String> violations) {
        if (weights == null) {
            violations.add("scoreWeights is required");
        } else if (weights.distance() < 0 || weights.lateness() < 0 || weights.unserved() <= 0) {
            violations.add("score weights must be non-negative and unserved must be positive");
        }
    }

    private static void validateConnectivity(Scenario scenario, List<String> violations) {
        Position anchor = scenario.vehicles().getFirst().start();
        GridPathfinder pathfinder = new GridPathfinder(scenario.grid());
        List<Position> required = new ArrayList<>();
        scenario.vehicles().forEach(vehicle -> required.add(vehicle.start()));
        scenario.tasks().forEach(task -> {
            required.add(task.pickup());
            required.add(task.delivery());
        });
        try {
            for (Position position : required) {
                pathfinder.findPath(anchor, position, PathAlgorithm.DIJKSTRA);
            }
        } catch (PathNotFoundException error) {
            violations.add("all vehicle starts, pickups, and deliveries must be connected");
        }
    }
}
