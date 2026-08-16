package dev.depotreplay.core.save;

import dev.depotreplay.core.engine.SimulationEngine;
import dev.depotreplay.core.io.CanonicalJson;
import dev.depotreplay.core.io.CorruptedReplayException;
import dev.depotreplay.core.model.DeliveryTaskDefinition;
import dev.depotreplay.core.model.DispatchCommand;
import dev.depotreplay.core.model.GridDefinition;
import dev.depotreplay.core.model.PathAlgorithm;
import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ScoreWeights;
import dev.depotreplay.core.model.ServiceAction;
import dev.depotreplay.core.model.VehicleDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SaveServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void restoresAResumableEngineWithTheIdenticalStateHash() {
        SimulationEngine engine = new SimulationEngine(scenario());
        engine.submitCommand(new DispatchCommand(0, "V1", "T1", ServiceAction.PICKUP));
        engine.advanceOneTick();
        SaveService service = new SaveService();
        SaveFile save = service.create(engine);
        Path path = temporaryDirectory.resolve("checkpoint.save.json");
        service.save(path, save);

        LoadedSave loaded = service.restore(service.load(path));
        assertEquals(engine.snapshot(), loaded.state());
        assertEquals(CanonicalJson.sha256(engine.snapshot()), loaded.stateHash());

        loaded.engine().submitCommand(new DispatchCommand(1, "V1", "T1", ServiceAction.DELIVER));
        assertEquals(0, loaded.engine().runUntilTerminal().score().unservedTasks());
    }

    @Test
    void rejectsAnAlteredSavedStateHash() {
        SimulationEngine engine = new SimulationEngine(scenario());
        SaveFile save = new SaveService().create(engine);
        SaveFile corrupted = new SaveFile(
                save.schemaVersion(), save.scenario(), save.scenarioHash(), save.savedAtTick(),
                save.commands(), "f".repeat(64)
        );
        assertThrows(CorruptedReplayException.class, () -> new SaveService().restore(corrupted));
    }

    private static Scenario scenario() {
        return new Scenario(
                1, "Save fixture", 9,
                new GridDefinition(4, 3, List.of()),
                List.of(
                        new VehicleDefinition("V1", new Position(0, 0), 2),
                        new VehicleDefinition("V2", new Position(3, 2), 2)
                ),
                List.of(new DeliveryTaskDefinition(
                        "T1", new Position(1, 0), new Position(2, 0), 1, 4
                )),
                new ScoreWeights(1, 5, 100), 8, PathAlgorithm.DIJKSTRA
        );
    }
}
