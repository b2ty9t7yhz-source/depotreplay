package dev.depotreplay.core.strategy;

import dev.depotreplay.core.model.DeliveryTaskDefinition;
import dev.depotreplay.core.model.GridDefinition;
import dev.depotreplay.core.model.PathAlgorithm;
import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ScoreWeights;
import dev.depotreplay.core.model.ServiceAction;
import dev.depotreplay.core.model.VehicleDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrategyRunnerTest {
    private final StrategyRunner runner = new StrategyRunner();

    @Test
    void everyBaselineCompletesTheScenarioThroughTheSharedEngine() {
        for (DispatchStrategy strategy : List.of(
                new NearestTaskStrategy(),
                new EarliestDeadlineFirstStrategy(),
                new CapacityAwareStrategy()
        )) {
            StrategyRun run = runner.run(scenario(), strategy);
            assertTrue(run.finalState().complete(), strategy.id());
            assertEquals(0, run.finalState().score().unservedTasks(), strategy.id());
        }
    }

    @Test
    void baselineRunsAreDeterministic() {
        StrategyRun first = runner.run(scenario(), new CapacityAwareStrategy());
        StrategyRun second = runner.run(scenario(), new CapacityAwareStrategy());
        assertEquals(first, second);
    }

    @Test
    void strategiesApplyTheirDocumentedPrimaryOrdering() {
        StrategyRun nearest = runner.run(scenario(), new NearestTaskStrategy());
        StrategyRun deadline = runner.run(scenario(), new EarliestDeadlineFirstStrategy());
        StrategyRun capacity = runner.run(scenario(), new CapacityAwareStrategy());

        assertEquals("NEAR", nearest.finalState().commandLog().getFirst().taskId());
        assertEquals("URGENT", deadline.finalState().commandLog().getFirst().taskId());
        assertEquals("LARGE", capacity.finalState().commandLog().getFirst().taskId());
        assertEquals(ServiceAction.PICKUP, capacity.finalState().commandLog().getFirst().action());
    }

    private static Scenario scenario() {
        return new Scenario(
                1,
                "Strategy fixture",
                20260813,
                new GridDefinition(8, 6, List.of(
                        new Position(3, 1), new Position(3, 2), new Position(3, 3), new Position(3, 4)
                )),
                List.of(
                        new VehicleDefinition("V1", new Position(0, 0), 4),
                        new VehicleDefinition("V2", new Position(7, 5), 4)
                ),
                List.of(
                        new DeliveryTaskDefinition("NEAR", new Position(1, 0), new Position(6, 0), 1, 22),
                        new DeliveryTaskDefinition("URGENT", new Position(2, 2), new Position(6, 2), 2, 10),
                        new DeliveryTaskDefinition("LARGE", new Position(2, 4), new Position(6, 4), 4, 28)
                ),
                new ScoreWeights(1, 5, 100),
                80,
                PathAlgorithm.ASTAR
        );
    }
}
