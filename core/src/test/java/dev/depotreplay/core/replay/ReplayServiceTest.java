package dev.depotreplay.core.replay;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplayServiceTest {
    private final ReplayService service = new ReplayService();

    @TempDir
    Path temporaryDirectory;

    @Test
    void replayProducesIdenticalVerifiedStateHashes() {
        ReplayFile replay = service.create(scenario(), commands());
        ReplayVerification verification = service.verify(replay);

        assertTrue(verification.verified());
        assertEquals(replay.finalStateHash(), verification.finalStateHash());
        assertEquals(replay.tickHashes().size(), verification.verifiedTicks());
        assertEquals(replay.finalStateHash(), CanonicalJson.sha256(verification.finalState()));
    }

    @Test
    void savesLoadsAndVerifiesTheSameCanonicalReplay() throws Exception {
        ReplayFile replay = service.create(scenario(), commands());
        Path path = temporaryDirectory.resolve("run.replay.json");
        service.save(path, replay);

        ReplayFile loaded = service.load(path);
        assertEquals(replay, loaded);
        assertEquals(replay.finalStateHash(), service.verify(loaded).finalStateHash());
        assertEquals(CanonicalJson.string(replay), Files.readString(path));
    }

    @Test
    void detectsCorruptedTickHashes() {
        ReplayFile replay = service.create(scenario(), commands());
        List<TickHash> hashes = new ArrayList<>(replay.tickHashes());
        TickHash original = hashes.get(1);
        hashes.set(1, new TickHash(original.tick(), "0".repeat(64)));
        ReplayFile corrupted = new ReplayFile(
                replay.schemaVersion(), replay.engineVersion(), replay.scenario(), replay.scenarioHash(),
                replay.commands(), hashes, replay.finalStateHash()
        );
        CorruptedReplayException error = assertThrows(
                CorruptedReplayException.class,
                () -> service.verify(corrupted)
        );
        assertTrue(error.getMessage().contains("tick 1"));
    }

    @Test
    void detectsScenarioAndCommandCorruption() {
        ReplayFile replay = service.create(scenario(), commands());
        Scenario altered = new Scenario(
                replay.scenario().schemaVersion(), "Altered", replay.scenario().seed(),
                replay.scenario().grid(), replay.scenario().vehicles(), replay.scenario().tasks(),
                replay.scenario().scoreWeights(), replay.scenario().maxTicks(), replay.scenario().pathAlgorithm()
        );
        ReplayFile corruptedScenario = new ReplayFile(
                replay.schemaVersion(), replay.engineVersion(), altered, replay.scenarioHash(),
                replay.commands(), replay.tickHashes(), replay.finalStateHash()
        );
        assertThrows(CorruptedReplayException.class, () -> service.verify(corruptedScenario));

        List<DispatchCommand> corruptedCommands = new ArrayList<>(replay.commands());
        corruptedCommands.set(0, new DispatchCommand(0, "UNKNOWN", "T1", ServiceAction.PICKUP));
        ReplayFile corruptedCommand = new ReplayFile(
                replay.schemaVersion(), replay.engineVersion(), replay.scenario(), replay.scenarioHash(),
                corruptedCommands, replay.tickHashes(), replay.finalStateHash()
        );
        assertThrows(CorruptedReplayException.class, () -> service.verify(corruptedCommand));
    }

    @Test
    void wrapsLateCommandDivergenceAsCorruption() {
        ReplayFile replay = service.create(scenario(), commands());
        List<DispatchCommand> corruptedCommands = new ArrayList<>(replay.commands());
        corruptedCommands.set(1, new DispatchCommand(1, "V2", "T1", ServiceAction.DELIVER));
        ReplayFile corrupted = new ReplayFile(
                replay.schemaVersion(), replay.engineVersion(), replay.scenario(), replay.scenarioHash(),
                corruptedCommands, replay.tickHashes(), replay.finalStateHash()
        );
        assertThrows(CorruptedReplayException.class, () -> service.verify(corrupted));
    }

    private static List<DispatchCommand> commands() {
        return List.of(
                new DispatchCommand(0, "V1", "T1", ServiceAction.PICKUP),
                new DispatchCommand(1, "V1", "T1", ServiceAction.DELIVER)
        );
    }

    private static Scenario scenario() {
        return new Scenario(
                1,
                "Replay fixture",
                8080,
                new GridDefinition(4, 3, List.of()),
                List.of(
                        new VehicleDefinition("V1", new Position(0, 0), 2),
                        new VehicleDefinition("V2", new Position(3, 2), 2)
                ),
                List.of(new DeliveryTaskDefinition(
                        "T1", new Position(1, 0), new Position(2, 0), 1, 3
                )),
                new ScoreWeights(1, 5, 100),
                8,
                PathAlgorithm.ASTAR
        );
    }
}
