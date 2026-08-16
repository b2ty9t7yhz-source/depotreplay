package dev.depotreplay.core.strategy;

import dev.depotreplay.core.model.ServiceAction;
import dev.depotreplay.core.model.SimulationSnapshot;
import dev.depotreplay.core.model.VehicleSnapshot;
import dev.depotreplay.core.path.GridPathfinder;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

/** Selects the closest currently actionable pickup or delivery. */
public final class NearestTaskStrategy implements DispatchStrategy {
    @Override
    public String id() {
        return "nearest-task";
    }

    @Override
    public Optional<StrategyDecision> chooseNext(
            SimulationSnapshot state,
            VehicleSnapshot vehicle,
            Set<String> reservedPendingTasks,
            GridPathfinder pathfinder
    ) {
        return StrategySupport.candidates(state, vehicle, reservedPendingTasks, pathfinder).stream()
                .min(Comparator.comparingInt(StrategySupport.Candidate::distance)
                        .thenComparing(candidate -> candidate.action() == ServiceAction.DELIVER ? 0 : 1)
                        .thenComparingInt(candidate -> candidate.task().deadline())
                        .thenComparing(candidate -> candidate.task().id()))
                .map(StrategySupport.Candidate::decision);
    }
}
