package dev.depotreplay.game;

import dev.depotreplay.core.engine.SimulationEngine;

import java.util.Objects;

public record PersistenceResult(
        SimulationEngine engine,
        String message,
        boolean reviewMode,
        String modeLabel
) {
    public PersistenceResult(SimulationEngine engine, String message) {
        this(engine, message, false, "PLAYER DISPATCH");
    }

    public PersistenceResult {
        Objects.requireNonNull(engine, "engine");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(modeLabel, "modeLabel");
    }
}
