package dev.depotreplay.core.generate;

import dev.depotreplay.core.model.DeliveryTaskDefinition;
import dev.depotreplay.core.model.GridDefinition;
import dev.depotreplay.core.model.PathAlgorithm;
import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ScoreWeights;
import dev.depotreplay.core.model.VehicleDefinition;
import dev.depotreplay.core.validation.ScenarioValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;

/** Reproducible scenario generation driven only by an explicit seed. */
public final class SeededScenarioGenerator {
    public static final String GENERATOR_ID = "seeded-grid-v1";

    public Scenario generate(long seed) {
        GridDefinition grid = new GridDefinition(10, 8, List.of(
                new Position(3, 1), new Position(3, 2), new Position(3, 3),
                new Position(6, 4), new Position(6, 5), new Position(6, 6)
        ));
        List<Position> available = new ArrayList<>();
        for (int x = 0; x < grid.width(); x++) {
            for (int y = 0; y < grid.height(); y++) {
                Position position = new Position(x, y);
                if (!grid.obstacles().contains(position)
                        && !position.equals(new Position(0, 0))
                        && !position.equals(new Position(9, 7))) {
                    available.add(position);
                }
            }
        }
        SplittableRandom random = new SplittableRandom(seed);
        shuffle(available, random);
        List<DeliveryTaskDefinition> tasks = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            tasks.add(new DeliveryTaskDefinition(
                    "TASK-" + (index + 1),
                    available.get(index * 2),
                    available.get(index * 2 + 1),
                    random.nextInt(1, 5),
                    random.nextInt(16, 41)
            ));
        }
        Scenario scenario = new Scenario(
                1,
                "Seeded Dispatch " + Long.toUnsignedString(seed),
                seed,
                grid,
                List.of(
                        new VehicleDefinition("VAN-A", new Position(0, 0), 4),
                        new VehicleDefinition("VAN-B", new Position(9, 7), 4)
                ),
                tasks,
                new ScoreWeights(1, 5, 100),
                80,
                PathAlgorithm.ASTAR
        );
        ScenarioValidator.validate(scenario);
        return scenario;
    }

    private static void shuffle(List<Position> positions, SplittableRandom random) {
        for (int index = positions.size() - 1; index > 0; index--) {
            int swapWith = random.nextInt(index + 1);
            Collections.swap(positions, index, swapWith);
        }
    }
}
