package dev.depotreplay.core.save;

import dev.depotreplay.core.engine.CommandRejectedException;
import dev.depotreplay.core.engine.SimulationEngine;
import dev.depotreplay.core.io.CanonicalJson;
import dev.depotreplay.core.io.CorruptedReplayException;
import dev.depotreplay.core.io.DepotReplayException;
import dev.depotreplay.core.model.SimulationSnapshot;

import java.nio.file.Path;

public final class SaveService {
    public SaveFile create(SimulationEngine engine) {
        SimulationSnapshot state = engine.snapshot();
        return new SaveFile(
                1,
                engine.scenario(),
                CanonicalJson.sha256(engine.scenario()),
                state.tick(),
                state.commandLog(),
                CanonicalJson.sha256(state)
        );
    }

    public LoadedSave restore(SaveFile save) {
        if (save == null || save.schemaVersion() != 1 || save.scenario() == null
                || save.scenarioHash() == null || save.commands() == null || save.stateHash() == null) {
            throw new CorruptedReplayException("Save file is missing required fields or has an unsupported schema");
        }
        String scenarioHash = CanonicalJson.sha256(save.scenario());
        if (!scenarioHash.equals(save.scenarioHash())) {
            throw new CorruptedReplayException("Save scenario hash mismatch");
        }
        try {
            SimulationEngine engine = new SimulationEngine(save.scenario());
            save.commands().forEach(engine::submitCommand);
            while (engine.currentTick() < save.savedAtTick()) {
                engine.advanceOneTick();
            }
            SimulationSnapshot state = engine.snapshot();
            String stateHash = CanonicalJson.sha256(state);
            if (!stateHash.equals(save.stateHash())) {
                throw new CorruptedReplayException(
                        "Save state hash mismatch: expected " + save.stateHash() + " but was " + stateHash
                );
            }
            return new LoadedSave(engine, state, stateHash);
        } catch (CommandRejectedException | IllegalStateException error) {
            if (error instanceof CorruptedReplayException corrupted) {
                throw corrupted;
            }
            throw new CorruptedReplayException("Save command history cannot be reconstructed", error);
        }
    }

    public SaveFile load(Path path) {
        try {
            return CanonicalJson.read(path, SaveFile.class);
        } catch (DepotReplayException error) {
            throw new CorruptedReplayException("Could not parse save file " + path, error);
        }
    }

    public void save(Path path, SaveFile save) {
        restore(save);
        CanonicalJson.write(path, save);
    }
}
