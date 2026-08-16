package dev.depotreplay.core.scoring;

import dev.depotreplay.core.model.ScoreBreakdown;
import dev.depotreplay.core.model.ScoreWeights;
import dev.depotreplay.core.model.TaskSnapshot;
import dev.depotreplay.core.model.TaskStatus;
import dev.depotreplay.core.model.VehicleSnapshot;

import java.util.List;

public final class Scorer {
    private Scorer() { }

    public static ScoreBreakdown calculate(
            List<VehicleSnapshot> vehicles,
            List<TaskSnapshot> tasks,
            ScoreWeights weights
    ) {
        int distance = vehicles.stream().mapToInt(VehicleSnapshot::distanceTraveled).sum();
        int lateness = tasks.stream()
                .filter(task -> task.status() == TaskStatus.DELIVERED)
                .mapToInt(task -> Math.max(0, task.deliveryTick() - task.deadline()))
                .sum();
        int unserved = (int) tasks.stream()
                .filter(task -> task.status() != TaskStatus.DELIVERED)
                .count();
        long distanceCost = Math.multiplyFull(distance, weights.distance());
        long latenessCost = Math.multiplyFull(lateness, weights.lateness());
        long unservedCost = Math.multiplyFull(unserved, weights.unserved());
        return new ScoreBreakdown(
                distance,
                lateness,
                unserved,
                distanceCost,
                latenessCost,
                unservedCost,
                Math.addExact(Math.addExact(distanceCost, latenessCost), unservedCost)
        );
    }
}
