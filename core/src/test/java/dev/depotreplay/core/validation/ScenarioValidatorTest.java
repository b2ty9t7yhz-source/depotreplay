package dev.depotreplay.core.validation;

import dev.depotreplay.core.model.DeliveryTaskDefinition;
import dev.depotreplay.core.model.GridDefinition;
import dev.depotreplay.core.model.PathAlgorithm;
import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ScoreWeights;
import dev.depotreplay.core.model.VehicleDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioValidatorTest {
    @Test
    void acceptsAValidTwoVehicleScenario() {
        assertDoesNotThrow(() -> ScenarioValidator.validate(validScenario()));
    }

    @Test
    void aggregatesInputErrors() {
        Scenario invalid = new Scenario(
                9,
                " ",
                7,
                new GridDefinition(1, 2, List.of(new Position(4, 4))),
                List.of(new VehicleDefinition("bad id", new Position(0, 0), 0)),
                List.of(new DeliveryTaskDefinition("T", new Position(0, 0), new Position(0, 0), 9, 0)),
                new ScoreWeights(-1, 1, 0),
                5,
                null
        );

        ValidationException error = assertThrows(
                ValidationException.class,
                () -> ScenarioValidator.validate(invalid)
        );
        assertTrue(error.violations().size() >= 8);
    }

    private static Scenario validScenario() {
        return new Scenario(
                1,
                "Validation fixture",
                7,
                new GridDefinition(5, 5, List.of(new Position(2, 2))),
                List.of(
                        new VehicleDefinition("V1", new Position(0, 0), 3),
                        new VehicleDefinition("V2", new Position(4, 4), 3)
                ),
                List.of(new DeliveryTaskDefinition(
                        "T1", new Position(0, 1), new Position(4, 3), 2, 20
                )),
                new ScoreWeights(1, 5, 100),
                50,
                PathAlgorithm.ASTAR
        );
    }
}
