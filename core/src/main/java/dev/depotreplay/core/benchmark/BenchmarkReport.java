package dev.depotreplay.core.benchmark;

import java.util.List;

/** Canonically serializable report for a deterministic multi-seed strategy comparison. */
public record BenchmarkReport(
        int schemaVersion,
        String engineVersion,
        String generatorId,
        long firstSeed,
        int scenarioCount,
        List<String> strategyIds,
        List<BenchmarkScenario> scenarios,
        List<BenchmarkSummary> summaries
) {
    public BenchmarkReport {
        strategyIds = List.copyOf(strategyIds);
        scenarios = List.copyOf(scenarios);
        summaries = List.copyOf(summaries);
    }
}
