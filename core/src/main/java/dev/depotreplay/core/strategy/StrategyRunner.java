package dev.depotreplay.core.strategy;

import dev.depotreplay.core.engine.SimulationEngine;
import dev.depotreplay.core.model.DispatchCommand;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ServiceAction;
import dev.depotreplay.core.model.SimulationSnapshot;
import dev.depotreplay.core.model.TaskStatus;
import dev.depotreplay.core.model.VehicleSnapshot;
import dev.depotreplay.core.path.GridPathfinder;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class StrategyRunner {
    public StrategyRun run(Scenario scenario, DispatchStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy");
        SimulationEngine engine = new SimulationEngine(scenario);
        GridPathfinder pathfinder = new GridPathfinder(scenario.grid());
        while (!engine.isTerminal()) {
            SimulationSnapshot state = engine.snapshot();
            Set<String> reserved = new HashSet<>();
            state.vehicles().stream()
                    .filter(vehicle -> vehicle.activeAction() == ServiceAction.PICKUP)
                    .map(VehicleSnapshot::activeTaskId)
                    .forEach(reserved::add);

            state.vehicles().stream()
                    .sorted(Comparator.comparing(VehicleSnapshot::id))
                    .filter(vehicle -> engine.isVehicleIdle(vehicle.id()))
                    .forEach(vehicle -> strategy.chooseNext(state, vehicle, reserved, pathfinder)
                            .ifPresent(decision -> {
                                engine.submitCommand(new DispatchCommand(
                                        state.tick(), vehicle.id(), decision.taskId(), decision.action()
                                ));
                                if (decision.action() == ServiceAction.PICKUP) {
                                    reserved.add(decision.taskId());
                                }
                            }));
            engine.advanceOneTick();
        }
        return new StrategyRun(strategy.id(), engine.snapshot());
    }
}
