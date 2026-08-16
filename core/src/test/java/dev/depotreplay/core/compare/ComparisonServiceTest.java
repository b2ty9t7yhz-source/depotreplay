package dev.depotreplay.core.compare;

import dev.depotreplay.core.model.DeliveryTaskDefinition;
import dev.depotreplay.core.model.GridDefinition;
import dev.depotreplay.core.model.PathAlgorithm;
import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ScoreWeights;
import dev.depotreplay.core.model.VehicleDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ComparisonServiceTest {
    @Test
    void exposesScoreBreakdownsRoutesAndCompletionTimesForEveryBaseline() {
        ComparisonReport report = new ComparisonService().compare(scenario(), null);
        assertEquals(3, report.runs().size());
        for (RunComparison run : report.runs()) {
            assertEquals(2, run.vehicles().size());
            assertEquals(1, run.tasks().size());
            assertFalse(run.vehicles().getFirst().route().isEmpty());
            assertEquals(64, run.finalStateHash().length());
        }
    }

    private static Scenario scenario() {
        return new Scenario(
                1, "Comparison fixture", 10,
                new GridDefinition(4, 3, List.of()),
                List.of(
                        new VehicleDefinition("V1", new Position(0, 0), 2),
                        new VehicleDefinition("V2", new Position(3, 2), 2)
                ),
                List.of(new DeliveryTaskDefinition(
                        "T1", new Position(1, 0), new Position(2, 0), 1, 4
                )),
                new ScoreWeights(1, 5, 100), 8, PathAlgorithm.ASTAR
        );
    }
}
