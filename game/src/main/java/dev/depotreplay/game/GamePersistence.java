package dev.depotreplay.game;

import dev.depotreplay.core.engine.SimulationEngine;
import dev.depotreplay.core.model.Scenario;

/** Platform boundary for save, load, and replay export. */
public interface GamePersistence {
    boolean available();

    PersistenceResult save(Scenario scenario, SimulationEngine engine);

    PersistenceResult load(Scenario scenario, SimulationEngine engine);

    PersistenceResult exportReplay(Scenario scenario, SimulationEngine engine);
}
