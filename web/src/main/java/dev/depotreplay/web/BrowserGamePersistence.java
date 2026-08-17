package dev.depotreplay.web;

import dev.depotreplay.core.engine.SimulationEngine;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.game.GamePersistence;
import dev.depotreplay.game.PersistenceResult;

final class BrowserGamePersistence implements GamePersistence {
    private static final String MESSAGE = "ERROR: Save files and replay export are available in the desktop app.";

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public PersistenceResult save(Scenario scenario, SimulationEngine engine) {
        return unavailable(engine);
    }

    @Override
    public PersistenceResult load(Scenario scenario, SimulationEngine engine) {
        return unavailable(engine);
    }

    @Override
    public PersistenceResult exportReplay(Scenario scenario, SimulationEngine engine) {
        return unavailable(engine);
    }

    private static PersistenceResult unavailable(SimulationEngine engine) {
        return new PersistenceResult(engine, MESSAGE);
    }
}
