package dev.depotreplay.core.model;

import java.util.List;

/** Complete, portable input to a deterministic simulation. */
public record Scenario(
        int schemaVersion,
        String name,
        long seed,
        GridDefinition grid,
        List<VehicleDefinition> vehicles,
        List<DeliveryTaskDefinition> tasks,
        ScoreWeights scoreWeights,
        int maxTicks,
        PathAlgorithm pathAlgorithm
) {
    public Scenario {
        vehicles = vehicles == null ? List.of() : List.copyOf(vehicles);
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }
}
