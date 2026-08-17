package dev.depotreplay.desktop;

import dev.depotreplay.core.engine.SimulationEngine;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.replay.ReplayFile;
import dev.depotreplay.core.replay.ReplayService;
import dev.depotreplay.core.save.LoadedSave;
import dev.depotreplay.core.save.SaveService;
import dev.depotreplay.game.GamePersistence;
import dev.depotreplay.game.PersistenceResult;

import java.nio.file.Path;

final class FileGamePersistence implements GamePersistence {
    private final Path savePath;
    private final Path replayPath;

    FileGamePersistence(Path dataDirectory) {
        savePath = dataDirectory.resolve("player.save.json");
        replayPath = dataDirectory.resolve("player.replay.json");
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public PersistenceResult save(Scenario scenario, SimulationEngine engine) {
        SaveService service = new SaveService();
        service.save(savePath, service.create(engine));
        return new PersistenceResult(engine, "Saved verified checkpoint to " + savePath + ".");
    }

    @Override
    public PersistenceResult load(Scenario scenario, SimulationEngine engine) {
        LoadedSave loaded = new SaveService().restore(new SaveService().load(savePath));
        if (!scenario.equals(loaded.engine().scenario())) {
            throw new IllegalArgumentException("Save belongs to a different scenario");
        }
        return new PersistenceResult(
                loaded.engine(),
                "Loaded checkpoint at tick " + loaded.state().tick() + "; hash verified."
        );
    }

    @Override
    public PersistenceResult exportReplay(Scenario scenario, SimulationEngine engine) {
        ReplayService service = new ReplayService();
        ReplayFile replay = service.create(scenario, engine.snapshot().commandLog());
        service.save(replayPath, replay);
        return new PersistenceResult(
                engine,
                "Exported and verified replay: " + replay.finalStateHash().substring(0, 12) + "..."
        );
    }
}
