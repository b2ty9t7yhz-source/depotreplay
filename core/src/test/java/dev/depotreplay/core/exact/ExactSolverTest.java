package dev.depotreplay.core.exact;

import dev.depotreplay.core.model.DeliveryTaskDefinition;
import dev.depotreplay.core.model.GridDefinition;
import dev.depotreplay.core.model.PathAlgorithm;
import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ScoreWeights;
import dev.depotreplay.core.model.VehicleDefinition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExactSolverTest {
    @Test
    void findsAndReplaysTheMinimumCostPlanForATinyScenario() {
        ExactSolution solution = new ExactSolver().solve(tinyScenario());

        assertTrue(solution.finalState().complete());
        assertEquals(2, solution.finalState().score().total());
        assertEquals(solution.commands(), solution.finalState().commandLog());
        assertTrue(solution.exploredStates() > 0);
    }

    @Test
    void canOptimallyLeaveAnExpensiveTaskUnserved() {
        Scenario base = tinyScenario();
        Scenario lowPenalty = new Scenario(
                base.schemaVersion(), base.name(), base.seed(), base.grid(), base.vehicles(), base.tasks(),
                new ScoreWeights(100, 0, 1), base.maxTicks(), base.pathAlgorithm()
        );

        ExactSolution solution = new ExactSolver().solve(lowPenalty);
        assertEquals(2, solution.finalState().score().unservedTasks());
        assertEquals(2, solution.finalState().score().total());
        assertTrue(solution.commands().isEmpty());
    }

    @Test
    void rejectsInputsBeyondThePublishedScaleLimit() {
        Scenario base = tinyScenario();
        List<DeliveryTaskDefinition> tasks = new ArrayList<>();
        for (int index = 0; index < ExactSolver.MAX_TASKS + 1; index++) {
            tasks.add(new DeliveryTaskDefinition(
                    "T" + index, new Position(0, index % 3), new Position(3, index % 3), 1, 10
            ));
        }
        Scenario tooLarge = new Scenario(
                base.schemaVersion(), base.name(), base.seed(), base.grid(), base.vehicles(), tasks,
                base.scoreWeights(), base.maxTicks(), base.pathAlgorithm()
        );
        assertThrows(ExactSolverLimitException.class, () -> new ExactSolver().solve(tooLarge));
    }

    private static Scenario tinyScenario() {
        return new Scenario(
                1,
                "Exact fixture",
                123,
                new GridDefinition(4, 3, List.of()),
                List.of(
                        new VehicleDefinition("V1", new Position(0, 0), 1),
                        new VehicleDefinition("V2", new Position(0, 2), 1)
                ),
                List.of(
                        new DeliveryTaskDefinition("T1", new Position(0, 0), new Position(1, 0), 1, 5),
                        new DeliveryTaskDefinition("T2", new Position(0, 2), new Position(1, 2), 1, 5)
                ),
                new ScoreWeights(1, 5, 100),
                20,
                PathAlgorithm.ASTAR
        );
    }
}
