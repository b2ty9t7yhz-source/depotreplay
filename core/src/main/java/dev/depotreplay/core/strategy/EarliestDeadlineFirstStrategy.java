package dev.depotreplay.core.strategy;

import dev.depotreplay.core.model.ServiceAction;
import dev.depotreplay.core.model.SimulationSnapshot;
import dev.depotreplay.core.model.VehicleSnapshot;
import dev.depotreplay.core.path.GridPathfinder;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

/** Selects the actionable task with the earliest delivery deadline. */
public final class EarliestDeadlineFirstStrategy implements DispatchStrategy {
    @Override
    public String id() {
        return "earliest-deadline-first";
    }

    @Override
    public Optional<StrategyDecision> chooseNext(
            SimulationSnapshot state,
            VehicleSnapshot vehicle,
            Set<String> reservedPendingTasks,
            GridPathfinder pathfinder
    ) {
        return StrategySupport.candidates(state, vehicle, reservedPendingTasks, pathfinder).stream()
                .min(Comparator.comparingInt((StrategySupport.Candidate candidate) -> candidate.task().deadline())
                        .thenComparing(candidate -> candidate.action() == ServiceAction.DELIVER ? 0 : 1)
                        .thenComparingInt(StrategySupport.Candidate::distance)
                        .thenComparing(candidate -> candidate.task().id()))
                .map(StrategySupport.Candidate::decision);
    }
}
