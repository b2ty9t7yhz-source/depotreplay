package dev.depotreplay.core.strategy;

import dev.depotreplay.core.model.ServiceAction;
import dev.depotreplay.core.model.SimulationSnapshot;
import dev.depotreplay.core.model.VehicleSnapshot;
import dev.depotreplay.core.path.GridPathfinder;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Fills available capacity with high-demand nearby pickups unless an onboard
 * delivery has insufficient slack for that detour.
 */
public final class CapacityAwareStrategy implements DispatchStrategy {
    @Override
    public String id() {
        return "capacity-aware";
    }

    @Override
    public Optional<StrategyDecision> chooseNext(
            SimulationSnapshot state,
            VehicleSnapshot vehicle,
            Set<String> reservedPendingTasks,
            GridPathfinder pathfinder
    ) {
        List<StrategySupport.Candidate> all = StrategySupport.candidates(
                state, vehicle, reservedPendingTasks, pathfinder
        );
        List<StrategySupport.Candidate> deliveries = all.stream()
                .filter(candidate -> candidate.action() == ServiceAction.DELIVER)
                .sorted(deliveryOrder())
                .toList();
        List<StrategySupport.Candidate> pickups = all.stream()
                .filter(candidate -> candidate.action() == ServiceAction.PICKUP)
                .sorted(pickupOrder())
                .toList();

        if (!deliveries.isEmpty()) {
            StrategySupport.Candidate urgent = deliveries.getFirst();
            int deliverySlack = urgent.task().deadline() - state.tick() - Math.max(1, urgent.distance());
            int pickupDetour = pickups.isEmpty() ? Integer.MAX_VALUE : Math.max(1, pickups.getFirst().distance());
            if (pickups.isEmpty() || deliverySlack <= pickupDetour) {
                return Optional.of(urgent.decision());
            }
        }
        if (!pickups.isEmpty()) {
            return Optional.of(pickups.getFirst().decision());
        }
        return deliveries.stream().findFirst().map(StrategySupport.Candidate::decision);
    }

    private static Comparator<StrategySupport.Candidate> deliveryOrder() {
        return Comparator.comparingInt((StrategySupport.Candidate candidate) -> candidate.task().deadline())
                .thenComparingInt(StrategySupport.Candidate::distance)
                .thenComparing(candidate -> candidate.task().id());
    }

    private static Comparator<StrategySupport.Candidate> pickupOrder() {
        return Comparator.comparingInt((StrategySupport.Candidate candidate) -> -candidate.task().demand())
                .thenComparingInt(StrategySupport.Candidate::distance)
                .thenComparingInt(candidate -> candidate.task().deadline())
                .thenComparing(candidate -> candidate.task().id());
    }
}
