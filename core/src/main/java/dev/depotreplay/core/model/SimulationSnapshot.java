package dev.depotreplay.core.model;

import java.util.List;

/** Immutable public view of all deterministic state. */
public record SimulationSnapshot(
        int schemaVersion,
        String scenarioName,
        long seed,
        int tick,
        int maxTicks,
        PathAlgorithm pathAlgorithm,
        GridDefinition grid,
        List<VehicleSnapshot> vehicles,
        List<TaskSnapshot> tasks,
        ScoreBreakdown score,
        boolean complete,
        List<DispatchCommand> commandLog
) {
    public SimulationSnapshot {
        vehicles = List.copyOf(vehicles);
        tasks = List.copyOf(tasks);
        commandLog = List.copyOf(commandLog);
    }
}
