package dev.depotreplay.core.benchmark;

import dev.depotreplay.core.model.ScoreBreakdown;

/** One strategy result for one generated scenario. Lower score is better. */
public record BenchmarkResult(
        String strategyId,
        ScoreBreakdown score,
        String finalStateHash
) { }
