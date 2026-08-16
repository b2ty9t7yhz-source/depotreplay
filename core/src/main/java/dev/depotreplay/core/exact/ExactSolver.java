package dev.depotreplay.core.exact;

import dev.depotreplay.core.engine.SimulationEngine;
import dev.depotreplay.core.model.DeliveryTaskDefinition;
import dev.depotreplay.core.model.DispatchCommand;
import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ScoreWeights;
import dev.depotreplay.core.model.ServiceAction;
import dev.depotreplay.core.model.SimulationSnapshot;
import dev.depotreplay.core.model.VehicleDefinition;
import dev.depotreplay.core.path.GridPathfinder;
import dev.depotreplay.core.validation.ScenarioValidator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exhaustive event-order search for deliberately small scenarios. It explores
 * every feasible pickup/delivery sequence and vehicle assignment under the
 * same shortest-path distances, capacity rules, tick timing, and score formula
 * used by {@link SimulationEngine}.
 */
public final class ExactSolver {
    public static final int MAX_TASKS = 5;
    public static final int MAX_GRID_CELLS = 400;
    public static final int MAX_TICKS = 200;
    public static final String SCALE_LIMIT = "at most 5 tasks, 2 vehicles, 400 grid cells, and 200 ticks";

    private Scenario scenario;
    private List<DeliveryTaskDefinition> tasks;
    private List<VehicleDefinition> vehicleDefinitions;
    private GridPathfinder pathfinder;
    private ScoreWeights weights;
    private final Map<RouteKey, Integer> distanceCache = new HashMap<>();
    private final Map<SearchKey, Long> bestPartialByState = new HashMap<>();
    private long bestScore;
    private List<DispatchCommand> bestCommands;
    private long exploredStates;

    public ExactSolution solve(Scenario input) {
        ScenarioValidator.validate(input);
        enforceLimits(input);
        scenario = input;
        tasks = input.tasks().stream().sorted(Comparator.comparing(DeliveryTaskDefinition::id)).toList();
        vehicleDefinitions = input.vehicles().stream().sorted(Comparator.comparing(VehicleDefinition::id)).toList();
        pathfinder = new GridPathfinder(input.grid());
        weights = input.scoreWeights();
        distanceCache.clear();
        bestPartialByState.clear();
        exploredStates = 0;
        bestScore = Math.multiplyFull(tasks.size(), weights.unserved());
        bestCommands = List.of();

        VehiclePlanState first = VehiclePlanState.start(vehicleDefinitions.get(0));
        VehiclePlanState second = VehiclePlanState.start(vehicleDefinitions.get(1));
        search(0, 0, first, second, 0, 0, new ArrayList<>());

        List<DispatchCommand> commands = bestCommands.stream()
                .sorted(Comparator.comparingInt(DispatchCommand::tick)
                        .thenComparing(DispatchCommand::vehicleId)
                        .thenComparing(DispatchCommand::taskId)
                        .thenComparing(DispatchCommand::action))
                .toList();
        SimulationEngine engine = new SimulationEngine(input);
        commands.forEach(engine::submitCommand);
        SimulationSnapshot finalState = engine.runUntilTerminal();
        if (finalState.score().total() != bestScore) {
            throw new IllegalStateException(
                    "Exact plan verification failed: search=" + bestScore
                            + ", simulation=" + finalState.score().total()
            );
        }
        return new ExactSolution("exact-small", SCALE_LIMIT, exploredStates, commands, finalState);
    }

    private void search(
            int pickedMask,
            int deliveredMask,
            VehiclePlanState first,
            VehiclePlanState second,
            long distanceCost,
            long latenessCost,
            List<DispatchCommand> commands
    ) {
        exploredStates++;
        long partialCost = Math.addExact(distanceCost, latenessCost);
        int unserved = tasks.size() - Integer.bitCount(deliveredMask);
        long finishScore = Math.addExact(partialCost, Math.multiplyFull(unserved, weights.unserved()));
        if (finishScore < bestScore) {
            bestScore = finishScore;
            bestCommands = List.copyOf(commands);
        }
        if (deliveredMask == (1 << tasks.size()) - 1 || partialCost >= bestScore) {
            return;
        }

        SearchKey key = new SearchKey(pickedMask, deliveredMask, first.key(), second.key());
        Long previousCost = bestPartialByState.putIfAbsent(key, partialCost);
        if (previousCost != null) {
            if (previousCost <= partialCost) {
                return;
            }
            bestPartialByState.put(key, partialCost);
        }

        VehiclePlanState[] states = {first, second};
        for (int vehicleIndex = 0; vehicleIndex < states.length; vehicleIndex++) {
            VehiclePlanState vehicle = states[vehicleIndex];
            for (int taskIndex = 0; taskIndex < tasks.size(); taskIndex++) {
                int bit = 1 << taskIndex;
                if ((vehicle.cargoMask() & bit) != 0) {
                    exploreDelivery(
                            pickedMask, deliveredMask, first, second, distanceCost, latenessCost,
                            commands, vehicleIndex, vehicle, taskIndex, bit
                    );
                }
            }
            for (int taskIndex = 0; taskIndex < tasks.size(); taskIndex++) {
                int bit = 1 << taskIndex;
                DeliveryTaskDefinition task = tasks.get(taskIndex);
                if ((pickedMask & bit) == 0 && vehicle.load() + task.demand() <= vehicle.capacity()) {
                    explorePickup(
                            pickedMask, deliveredMask, first, second, distanceCost, latenessCost,
                            commands, vehicleIndex, vehicle, taskIndex, bit
                    );
                }
            }
        }
    }

    private void explorePickup(
            int pickedMask,
            int deliveredMask,
            VehiclePlanState first,
            VehiclePlanState second,
            long distanceCost,
            long latenessCost,
            List<DispatchCommand> commands,
            int vehicleIndex,
            VehiclePlanState vehicle,
            int taskIndex,
            int bit
    ) {
        DeliveryTaskDefinition task = tasks.get(taskIndex);
        int distance = distance(vehicle.position(), task.pickup());
        int completion = Math.addExact(vehicle.time(), Math.max(1, distance));
        if (completion > scenario.maxTicks()) {
            return;
        }
        VehiclePlanState next = new VehiclePlanState(
                vehicle.id(), task.pickup(), completion, vehicle.capacity(),
                vehicle.load() + task.demand(), vehicle.cargoMask() | bit
        );
        commands.add(new DispatchCommand(vehicle.time(), vehicle.id(), task.id(), ServiceAction.PICKUP));
        search(
                pickedMask | bit,
                deliveredMask,
                vehicleIndex == 0 ? next : first,
                vehicleIndex == 1 ? next : second,
                Math.addExact(distanceCost, Math.multiplyFull(distance, weights.distance())),
                latenessCost,
                commands
        );
        commands.removeLast();
    }

    private void exploreDelivery(
            int pickedMask,
            int deliveredMask,
            VehiclePlanState first,
            VehiclePlanState second,
            long distanceCost,
            long latenessCost,
            List<DispatchCommand> commands,
            int vehicleIndex,
            VehiclePlanState vehicle,
            int taskIndex,
            int bit
    ) {
        DeliveryTaskDefinition task = tasks.get(taskIndex);
        int distance = distance(vehicle.position(), task.delivery());
        int completion = Math.addExact(vehicle.time(), Math.max(1, distance));
        if (completion > scenario.maxTicks()) {
            return;
        }
        int rawLateness = Math.max(0, completion - task.deadline());
        VehiclePlanState next = new VehiclePlanState(
                vehicle.id(), task.delivery(), completion, vehicle.capacity(),
                vehicle.load() - task.demand(), vehicle.cargoMask() & ~bit
        );
        commands.add(new DispatchCommand(vehicle.time(), vehicle.id(), task.id(), ServiceAction.DELIVER));
        search(
                pickedMask,
                deliveredMask | bit,
                vehicleIndex == 0 ? next : first,
                vehicleIndex == 1 ? next : second,
                Math.addExact(distanceCost, Math.multiplyFull(distance, weights.distance())),
                Math.addExact(latenessCost, Math.multiplyFull(rawLateness, weights.lateness())),
                commands
        );
        commands.removeLast();
    }

    private int distance(Position from, Position to) {
        return distanceCache.computeIfAbsent(
                new RouteKey(from, to),
                ignored -> pathfinder.distance(from, to, scenario.pathAlgorithm())
        );
    }

    private static void enforceLimits(Scenario scenario) {
        int cells = Math.multiplyExact(scenario.grid().width(), scenario.grid().height());
        if (scenario.tasks().size() > MAX_TASKS
                || scenario.vehicles().size() != 2
                || cells > MAX_GRID_CELLS
                || scenario.maxTicks() > MAX_TICKS) {
            throw new ExactSolverLimitException("Exact solver supports " + SCALE_LIMIT);
        }
    }

    private record RouteKey(Position from, Position to) { }

    private record SearchKey(int pickedMask, int deliveredMask, VehicleKey first, VehicleKey second) { }

    private record VehicleKey(Position position, int time, int load, int cargoMask) { }

    private record VehiclePlanState(
            String id,
            Position position,
            int time,
            int capacity,
            int load,
            int cargoMask
    ) {
        static VehiclePlanState start(VehicleDefinition definition) {
            return new VehiclePlanState(
                    definition.id(), definition.start(), 0, definition.capacity(), 0, 0
            );
        }

        VehicleKey key() {
            return new VehicleKey(position, time, load, cargoMask);
        }
    }
}
