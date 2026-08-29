package dev.depotreplay.core.benchmark;

import java.util.List;

/** Reproducible scenario identity and all strategy outcomes for that scenario. */
public record BenchmarkScenario(
        long seed,
        String scenarioName,
        String scenarioHash,
        List<BenchmarkResult> results
) {
    public BenchmarkScenario {
        results = List.copyOf(results);
    }
}
