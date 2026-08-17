package dev.depotreplay.core.scenario;

import dev.depotreplay.core.model.DeliveryTaskDefinition;
import dev.depotreplay.core.model.GridDefinition;
import dev.depotreplay.core.model.PathAlgorithm;
import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ScoreWeights;
import dev.depotreplay.core.model.VehicleDefinition;
import dev.depotreplay.core.validation.ScenarioValidator;

import java.util.List;

/** Portable built-in scenarios used when an external JSON file is unavailable. */
public final class BuiltinScenarios {
    private BuiltinScenarios() { }

    public static Scenario cityGrid() {
        Scenario scenario = new Scenario(
                1,
                "Harbor District Dispatch",
                20260813L,
                new GridDefinition(12, 9, List.of(
                        point(4, 1), point(4, 2), point(4, 3),
                        point(4, 5), point(4, 6), point(4, 7),
                        point(8, 1), point(8, 2), point(8, 3),
                        point(8, 4), point(8, 6), point(8, 7)
                )),
                List.of(
                        new VehicleDefinition("VAN-A", point(0, 0), 4),
                        new VehicleDefinition("VAN-B", point(11, 8), 4)
                ),
                List.of(
                        task("MEDS", 1, 2, 10, 1, 1, 22),
                        task("BOOKS", 2, 7, 9, 7, 2, 18),
                        task("TOOLS", 6, 0, 1, 8, 3, 30),
                        task("FOOD", 10, 5, 5, 5, 2, 14),
                        task("PARTS", 6, 8, 10, 3, 4, 25)
                ),
                new ScoreWeights(1, 5, 100),
                60,
                PathAlgorithm.ASTAR
        );
        ScenarioValidator.validate(scenario);
        return scenario;
    }

    private static Position point(int x, int y) {
        return new Position(x, y);
    }

    private static DeliveryTaskDefinition task(
            String id,
            int pickupX,
            int pickupY,
            int deliveryX,
            int deliveryY,
            int demand,
            int deadline
    ) {
        return new DeliveryTaskDefinition(
                id,
                point(pickupX, pickupY),
                point(deliveryX, deliveryY),
                demand,
                deadline
        );
    }
}
