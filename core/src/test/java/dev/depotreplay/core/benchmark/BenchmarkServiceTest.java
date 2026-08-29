package dev.depotreplay.core.benchmark;

import dev.depotreplay.core.io.CanonicalJson;
import dev.depotreplay.core.generate.SeededScenarioGenerator;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.strategy.Strategies;
import dev.depotreplay.core.strategy.StrategyRunner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchmarkServiceTest {
    private final BenchmarkService service = new BenchmarkService();

    @Test
    void identicalSeedRangesProduceIdenticalCanonicalReports() {
        BenchmarkReport first = service.run(40, 4);
        BenchmarkReport second = service.run(40, 4);

        assertEquals(first, second);
        assertEquals(CanonicalJson.sha256(first), CanonicalJson.sha256(second));
        assertEquals(4, first.scenarios().size());
        assertEquals(3, first.summaries().size());
        assertTrue(first.scenarios().stream()
                .allMatch(scenario -> scenario.results().size() == 3));
    }

    @Test
    void changedSeedRangeChangesTheCanonicalReport() {
        assertNotEquals(
                CanonicalJson.sha256(service.run(40, 2)),
                CanonicalJson.sha256(service.run(41, 2))
        );
    }

    @Test
    void summariesMatchTheRecordedPerScenarioResults() {
        BenchmarkReport report = service.run(-2, 5);

        report.summaries().forEach(summary -> {
            List<BenchmarkResult> strategyResults = report.scenarios().stream()
                    .flatMap(scenario -> scenario.results().stream())
                    .filter(result -> result.strategyId().equals(summary.strategyId()))
                    .toList();
            long expectedBestScores = report.scenarios().stream()
                    .filter(scenario -> isBest(summary.strategyId(), scenario))
                    .count();
            assertEquals(
                    strategyResults.stream().mapToLong(result -> result.score().total()).sum(),
                    summary.totalScore()
            );
            assertEquals(
                    strategyResults.stream().mapToLong(result -> result.score().distance()).sum(),
                    summary.totalDistance()
            );
            assertEquals(
                    strategyResults.stream().mapToLong(result -> result.score().lateness()).sum(),
                    summary.totalLateness()
            );
            assertEquals(
                    strategyResults.stream().mapToLong(result -> result.score().unservedTasks()).sum(),
                    summary.totalUnservedTasks()
            );
            assertEquals(expectedBestScores, summary.bestScoreCount());
        });
    }

    @Test
    void recordsHashesFromTheActualGeneratedScenarioAndFinalStates() {
        BenchmarkScenario recorded = service.run(14, 1).scenarios().getFirst();
        Scenario scenario = new SeededScenarioGenerator().generate(14);

        assertEquals(CanonicalJson.sha256(scenario), recorded.scenarioHash());
        recorded.results().forEach(result -> assertEquals(
                CanonicalJson.sha256(new StrategyRunner()
                        .run(scenario, Strategies.named(result.strategyId()))
                        .finalState()),
                result.finalStateHash()
        ));
    }

    @Test
    void verificationRegeneratesAndRejectsAlteredResults() {
        BenchmarkReport report = service.run(9, 2);
        assertEquals(CanonicalJson.sha256(report), service.verify(report));

        BenchmarkReport altered = new BenchmarkReport(
                report.schemaVersion(),
                report.engineVersion(),
                report.generatorId(),
                report.firstSeed() + 1,
                report.scenarioCount(),
                report.strategyIds(),
                report.scenarios(),
                report.summaries()
        );
        assertThrows(IllegalArgumentException.class, () -> service.verify(altered));
    }

    @Test
    void rejectsUnsafeScenarioCountsAndOverflowingSeedRanges() {
        assertThrows(IllegalArgumentException.class, () -> service.run(0, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.run(0, BenchmarkService.MAX_SCENARIOS + 1)
        );
        assertThrows(IllegalArgumentException.class, () -> service.run(Long.MAX_VALUE, 2));
    }

    private static boolean isBest(String strategyId, BenchmarkScenario scenario) {
        long bestScore = scenario.results().stream()
                .mapToLong(result -> result.score().total())
                .min()
                .orElseThrow();
        return scenario.results().stream()
                .filter(result -> result.strategyId().equals(strategyId))
                .anyMatch(result -> result.score().total() == bestScore);
    }
}
