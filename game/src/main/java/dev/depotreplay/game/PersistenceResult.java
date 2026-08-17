package dev.depotreplay.game;

import dev.depotreplay.core.engine.SimulationEngine;

import java.util.Objects;

public record PersistenceResult(SimulationEngine engine, String message) {
    public PersistenceResult {
        Objects.requireNonNull(engine, "engine");
        Objects.requireNonNull(message, "message");
    }
}
