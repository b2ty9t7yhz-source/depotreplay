package dev.depotreplay.core.engine;

import dev.depotreplay.core.model.DeliveryTaskDefinition;
import dev.depotreplay.core.model.DispatchCommand;
import dev.depotreplay.core.model.GridDefinition;
import dev.depotreplay.core.model.PathAlgorithm;
import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ScoreWeights;
import dev.depotreplay.core.model.ServiceAction;
import dev.depotreplay.core.model.SimulationSnapshot;
import dev.depotreplay.core.model.TaskStatus;
import dev.depotreplay.core.model.VehicleDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationEngineTest {
    @Test
    void advancesWithFixedTicksAndScoresDistanceLatenessAndUnservedTasks() {
        SimulationEngine engine = new SimulationEngine(scenario(1));
        engine.submitCommand(new DispatchCommand(0, "V1", "T1", ServiceAction.PICKUP));
        engine.advanceOneTick();
        assertEquals(TaskStatus.IN_TRANSIT, engine.snapshot().tasks().getFirst().status());

        engine.submitCommand(new DispatchCommand(1, "V1", "T1", ServiceAction.DELIVER));
        SimulationSnapshot completed = engine.runUntilTerminal();

        assertTrue(completed.complete());
        assertEquals(2, completed.tick());
        assertEquals(2, completed.score().distance());
        assertEquals(1, completed.score().lateness());
        assertEquals(0, completed.score().unservedTasks());
        assertEquals(7, completed.score().total());
        assertEquals(List.of(
                new Position(0, 0),
                new Position(1, 0),
                new Position(2, 0)
        ), completed.vehicles().getFirst().routeTrace());
    }

    @Test
    void commandOrderingAndResultsAreRepeatable() {
        SimulationSnapshot first = runOnce();
        SimulationSnapshot second = runOnce();
        assertEquals(first, second);
        assertEquals(List.of("V1", "V2"), first.commandLog().stream()
                .map(DispatchCommand::vehicleId)
                .toList());
    }

    @Test
    void rejectsCapacityViolationsAndDuplicateCommands() {
        SimulationEngine engine = new SimulationEngine(scenario(0));
        engine.submitCommand(new DispatchCommand(0, "V1", "T1", ServiceAction.PICKUP));
        assertThrows(
                CommandRejectedException.class,
                () -> engine.submitCommand(new DispatchCommand(0, "V1", "T1", ServiceAction.PICKUP))
        );

        SimulationEngine tooSmall = new SimulationEngine(scenarioWithSmallFirstVehicle());
        tooSmall.submitCommand(new DispatchCommand(0, "V1", "T1", ServiceAction.PICKUP));
        assertThrows(CommandRejectedException.class, tooSmall::advanceOneTick);
    }

    @Test
    void reportsUnservedPenaltyAtTheTickLimit() {
        SimulationEngine engine = new SimulationEngine(scenario(1));
        while (!engine.isTerminal()) {
            engine.advanceOneTick();
        }
        assertFalse(engine.snapshot().complete());
        assertEquals(1, engine.snapshot().score().unservedTasks());
        assertEquals(100, engine.snapshot().score().total());
    }

    @Test
    void rejectsAnInvalidBatchWithoutPartiallyApplyingEarlierCommands() {
        SimulationEngine engine = new SimulationEngine(scenario(1));
        engine.submitCommand(new DispatchCommand(0, "V1", "T1", ServiceAction.PICKUP));
        engine.submitCommand(new DispatchCommand(0, "V2", "T1", ServiceAction.PICKUP));

        assertThrows(CommandRejectedException.class, engine::advanceOneTick);
        assertEquals(0, engine.currentTick());
        assertTrue(engine.snapshot().commandLog().isEmpty());
        assertEquals(TaskStatus.PENDING, engine.snapshot().tasks().getFirst().status());
    }

    private static SimulationSnapshot runOnce() {
        SimulationEngine engine = new SimulationEngine(twoTaskScenario());
        engine.submitCommand(new DispatchCommand(0, "V2", "T2", ServiceAction.PICKUP));
        engine.submitCommand(new DispatchCommand(0, "V1", "T1", ServiceAction.PICKUP));
        engine.advanceOneTick();
        return engine.snapshot();
    }

    private static Scenario scenario(int deadline) {
        return new Scenario(
                1,
                "Engine fixture",
                42,
                new GridDefinition(4, 3, List.of()),
                List.of(
                        new VehicleDefinition("V1", new Position(0, 0), 2),
                        new VehicleDefinition("V2", new Position(3, 2), 2)
                ),
                List.of(new DeliveryTaskDefinition(
                        "T1", new Position(1, 0), new Position(2, 0), 2, deadline == 0 ? 1 : deadline
                )),
                new ScoreWeights(1, 5, 100),
                8,
                PathAlgorithm.ASTAR
        );
    }

    private static Scenario scenarioWithSmallFirstVehicle() {
        Scenario base = scenario(1);
        return new Scenario(
                base.schemaVersion(), base.name(), base.seed(), base.grid(),
                List.of(
                        new VehicleDefinition("V1", new Position(0, 0), 1),
                        new VehicleDefinition("V2", new Position(3, 2), 2)
                ),
                base.tasks(), base.scoreWeights(), base.maxTicks(), base.pathAlgorithm()
        );
    }

    private static Scenario twoTaskScenario() {
        return new Scenario(
                1,
                "Ordering fixture",
                42,
                new GridDefinition(4, 3, List.of()),
                List.of(
                        new VehicleDefinition("V1", new Position(0, 0), 2),
                        new VehicleDefinition("V2", new Position(3, 2), 2)
                ),
                List.of(
                        new DeliveryTaskDefinition("T1", new Position(1, 0), new Position(2, 0), 1, 7),
                        new DeliveryTaskDefinition("T2", new Position(2, 2), new Position(1, 2), 1, 7)
                ),
                new ScoreWeights(1, 5, 100),
                8,
                PathAlgorithm.DIJKSTRA
        );
    }
}
