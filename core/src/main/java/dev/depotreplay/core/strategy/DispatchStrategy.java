package dev.depotreplay.core.strategy;

import dev.depotreplay.core.model.SimulationSnapshot;
import dev.depotreplay.core.model.VehicleSnapshot;
import dev.depotreplay.core.path.GridPathfinder;

import java.util.Optional;
import java.util.Set;

public interface DispatchStrategy {
    String id();

    Optional<StrategyDecision> chooseNext(
            SimulationSnapshot state,
            VehicleSnapshot vehicle,
            Set<String> reservedPendingTasks,
            GridPathfinder pathfinder
    );
}
