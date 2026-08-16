package dev.depotreplay.core.compare;

import dev.depotreplay.core.io.CanonicalJson;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.SimulationSnapshot;
import dev.depotreplay.core.model.TaskSnapshot;
import dev.depotreplay.core.model.TaskStatus;
import dev.depotreplay.core.model.VehicleSnapshot;
import dev.depotreplay.core.strategy.Strategies;
import dev.depotreplay.core.strategy.StrategyRunner;

import java.util.ArrayList;
import java.util.List;

public final class ComparisonService {
    public ComparisonReport compare(Scenario scenario, SimulationSnapshot playerState) {
        List<RunComparison> runs = new ArrayList<>();
        if (playerState != null) {
            if (!scenario.name().equals(playerState.scenarioName())) {
                throw new IllegalArgumentException("Player state does not belong to the comparison scenario");
            }
            runs.add(toRun("player", playerState));
        }
        StrategyRunner runner = new StrategyRunner();
        Strategies.baselines().forEach(strategy -> runs.add(toRun(
                strategy.id(), runner.run(scenario, strategy).finalState()
        )));
        return new ComparisonReport(scenario.name(), runs);
    }

    public static RunComparison toRun(String label, SimulationSnapshot state) {
        List<VehicleComparison> vehicles = state.vehicles().stream()
                .map(ComparisonService::toVehicle)
                .toList();
        List<TaskCompletion> tasks = state.tasks().stream()
                .map(ComparisonService::toTask)
                .toList();
        return new RunComparison(
                label,
                state.score(),
                vehicles,
                tasks,
                CanonicalJson.sha256(state)
        );
    }

    private static VehicleComparison toVehicle(VehicleSnapshot vehicle) {
        return new VehicleComparison(vehicle.id(), vehicle.distanceTraveled(), vehicle.routeTrace());
    }

    private static TaskCompletion toTask(TaskSnapshot task) {
        int lateness = task.status() == TaskStatus.DELIVERED
                ? Math.max(0, task.deliveryTick() - task.deadline())
                : 0;
        return new TaskCompletion(
                task.id(), task.status(), task.vehicleId(), task.pickupTick(), task.deliveryTick(),
                task.deadline(), lateness
        );
    }
}
