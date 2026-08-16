package dev.depotreplay.core.strategy;

import dev.depotreplay.core.model.SimulationSnapshot;

public record StrategyRun(String strategyId, SimulationSnapshot finalState) { }
