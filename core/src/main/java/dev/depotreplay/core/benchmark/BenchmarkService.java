package dev.depotreplay.core.benchmark;

import dev.depotreplay.core.engine.SimulationContract;
import dev.depotreplay.core.generate.SeededScenarioGenerator;
import dev.depotreplay.core.io.CanonicalJson;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ScoreBreakdown;
import dev.depotreplay.core.strategy.DispatchStrategy;
import dev.depotreplay.core.strategy.Strategies;
import dev.depotreplay.core.strategy.StrategyRun;
import dev.depotreplay.core.strategy.StrategyRunner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Runs the built-in baselines over a consecutive, reproducible seed range. */
public final class BenchmarkService {
    public static final int MAX_SCENARIOS = 1_000;
    private static final int SCHEMA_VERSION = 1;

    public BenchmarkReport run(long firstSeed, int scenarioCount) {
        validateRange(firstSeed, scenarioCount);
        List<DispatchStrategy> strategies = Strategies.baselines();
        Map<String, MutableSummary> aggregates = new LinkedHashMap<>();
        strategies.forEach(strategy -> aggregates.put(strategy.id(), new MutableSummary()));

        SeededScenarioGenerator generator = new SeededScenarioGenerator();
        StrategyRunner runner = new StrategyRunner();
        List<BenchmarkScenario> scenarios = new ArrayList<>(scenarioCount);
        for (int index = 0; index < scenarioCount; index++) {
            long seed = Math.addExact(firstSeed, index);
            Scenario scenario = generator.generate(seed);
            List<BenchmarkResult> results = strategies.stream()
                    .map(strategy -> runStrategy(scenario, strategy, runner))
                    .toList();
            long bestScore = results.stream()
                    .mapToLong(result -> result.score().total())
                    .min()
                    .orElseThrow();
            results.forEach(result -> aggregates.get(result.strategyId())
                    .add(result.score(), result.score().total() == bestScore));
            scenarios.add(new BenchmarkScenario(
                    seed,
                    scenario.name(),
                    CanonicalJson.sha256(scenario),
                    results
            ));
        }

        List<String> strategyIds = strategies.stream().map(DispatchStrategy::id).toList();
        List<BenchmarkSummary> summaries = strategyIds.stream()
                .map(strategyId -> aggregates.get(strategyId).toSummary(strategyId, scenarioCount))
                .toList();
        return new BenchmarkReport(
                SCHEMA_VERSION,
                SimulationContract.VERSION,
                SeededScenarioGenerator.GENERATOR_ID,
                firstSeed,
                scenarioCount,
                strategyIds,
                scenarios,
                summaries
        );
    }

    public String verify(BenchmarkReport report) {
        Objects.requireNonNull(report, "report");
        if (report.schemaVersion() != SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported benchmark schema version: " + report.schemaVersion()
            );
        }
        if (!SimulationContract.VERSION.equals(report.engineVersion())) {
            throw new IllegalArgumentException(
                    "Unsupported benchmark engine version: " + report.engineVersion()
            );
        }
        if (!SeededScenarioGenerator.GENERATOR_ID.equals(report.generatorId())) {
            throw new IllegalArgumentException(
                    "Unsupported benchmark generator: " + report.generatorId()
            );
        }
        BenchmarkReport expected = run(report.firstSeed(), report.scenarioCount());
        if (!expected.equals(report)) {
            throw new IllegalArgumentException(
                    "Benchmark report does not match regenerated scenarios and strategy results"
            );
        }
        return CanonicalJson.sha256(expected);
    }

    private static BenchmarkResult runStrategy(
            Scenario scenario,
            DispatchStrategy strategy,
            StrategyRunner runner
    ) {
        StrategyRun run = runner.run(scenario, strategy);
        return new BenchmarkResult(
                strategy.id(),
                run.finalState().score(),
                CanonicalJson.sha256(run.finalState())
        );
    }

    private static void validateRange(long firstSeed, int scenarioCount) {
        if (scenarioCount < 1 || scenarioCount > MAX_SCENARIOS) {
            throw new IllegalArgumentException(
                    "scenario count must be between 1 and " + MAX_SCENARIOS
            );
        }
        try {
            Math.addExact(firstSeed, scenarioCount - 1L);
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("seed range exceeds signed 64-bit integers", error);
        }
    }

    private static final class MutableSummary {
        private int bestScoreCount;
        private long totalScore;
        private long totalDistance;
        private long totalLateness;
        private long totalUnservedTasks;

        void add(ScoreBreakdown score, boolean bestScore) {
            if (bestScore) {
                bestScoreCount++;
            }
            totalScore = Math.addExact(totalScore, score.total());
            totalDistance = Math.addExact(totalDistance, score.distance());
            totalLateness = Math.addExact(totalLateness, score.lateness());
            totalUnservedTasks = Math.addExact(totalUnservedTasks, score.unservedTasks());
        }

        BenchmarkSummary toSummary(String strategyId, int scenarioCount) {
            return new BenchmarkSummary(
                    strategyId,
                    scenarioCount,
                    bestScoreCount,
                    totalScore,
                    totalDistance,
                    totalLateness,
                    totalUnservedTasks
            );
        }
    }
}
