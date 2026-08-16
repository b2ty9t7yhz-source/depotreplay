package dev.depotreplay.core.save;

import dev.depotreplay.core.engine.SimulationEngine;
import dev.depotreplay.core.model.SimulationSnapshot;

public record LoadedSave(SimulationEngine engine, SimulationSnapshot state, String stateHash) { }
