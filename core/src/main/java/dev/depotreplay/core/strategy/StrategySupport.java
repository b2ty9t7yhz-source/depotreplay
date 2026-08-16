package dev.depotreplay.core.strategy;

import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.ServiceAction;
import dev.depotreplay.core.model.SimulationSnapshot;
import dev.depotreplay.core.model.TaskSnapshot;
import dev.depotreplay.core.model.TaskStatus;
import dev.depotreplay.core.model.VehicleSnapshot;
import dev.depotreplay.core.path.GridPathfinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class StrategySupport {
    private StrategySupport() { }

    static List<Candidate> candidates(
            SimulationSnapshot state,
            VehicleSnapshot vehicle,
            Set<String> reservedPendingTasks,
            GridPathfinder pathfinder
    ) {
        List<Candidate> candidates = new ArrayList<>();
        for (TaskSnapshot task : state.tasks()) {
            if (task.status() == TaskStatus.IN_TRANSIT && vehicle.id().equals(task.vehicleId())) {
                candidates.add(candidate(task, ServiceAction.DELIVER, task.delivery(), state, vehicle, pathfinder));
            } else if (task.status() == TaskStatus.PENDING
                    && !reservedPendingTasks.contains(task.id())
                    && vehicle.load() + task.demand() <= vehicle.capacity()) {
                candidates.add(candidate(task, ServiceAction.PICKUP, task.pickup(), state, vehicle, pathfinder));
            }
        }
        return candidates;
    }

    static Candidate candidate(
            TaskSnapshot task,
            ServiceAction action,
            Position target,
            SimulationSnapshot state,
            VehicleSnapshot vehicle,
            GridPathfinder pathfinder
    ) {
        return new Candidate(
                task,
                action,
                pathfinder.distance(vehicle.position(), target, state.pathAlgorithm())
        );
    }

    record Candidate(TaskSnapshot task, ServiceAction action, int distance) {
        StrategyDecision decision() {
            return new StrategyDecision(task.id(), action);
        }
    }
}
