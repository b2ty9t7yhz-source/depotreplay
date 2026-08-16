package dev.depotreplay.core.strategy;

import dev.depotreplay.core.model.ServiceAction;

public record StrategyDecision(String taskId, ServiceAction action) { }
