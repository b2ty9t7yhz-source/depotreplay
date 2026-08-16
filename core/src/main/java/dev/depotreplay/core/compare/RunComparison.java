package dev.depotreplay.core.compare;

import dev.depotreplay.core.model.ScoreBreakdown;

import java.util.List;

public record RunComparison(
        String label,
        ScoreBreakdown score,
        List<VehicleComparison> vehicles,
        List<TaskCompletion> tasks,
        String finalStateHash
) {
    public RunComparison {
        vehicles = List.copyOf(vehicles);
        tasks = List.copyOf(tasks);
    }
}
