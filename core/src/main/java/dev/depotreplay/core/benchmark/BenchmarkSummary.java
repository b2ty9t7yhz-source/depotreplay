package dev.depotreplay.core.benchmark;

/** Integer-only aggregate that remains stable across platforms and JSON implementations. */
public record BenchmarkSummary(
        String strategyId,
        int scenarioCount,
        int bestScoreCount,
        long totalScore,
        long totalDistance,
        long totalLateness,
        long totalUnservedTasks
) { }
