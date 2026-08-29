package dev.depotreplay.cli;

import dev.depotreplay.core.benchmark.BenchmarkReport;
import dev.depotreplay.core.benchmark.BenchmarkService;
import dev.depotreplay.core.benchmark.BenchmarkSummary;
import dev.depotreplay.core.compare.ComparisonReport;
import dev.depotreplay.core.compare.ComparisonService;
import dev.depotreplay.core.compare.RunComparison;
import dev.depotreplay.core.exact.ExactSolution;
import dev.depotreplay.core.exact.ExactSolver;
import dev.depotreplay.core.generate.SeededScenarioGenerator;
import dev.depotreplay.core.io.CanonicalJson;
import dev.depotreplay.core.io.CorruptedReplayException;
import dev.depotreplay.core.io.DepotReplayException;
import dev.depotreplay.core.io.ScenarioIo;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.replay.ReplayFile;
import dev.depotreplay.core.replay.ReplayService;
import dev.depotreplay.core.replay.ReplayVerification;
import dev.depotreplay.core.strategy.DispatchStrategy;
import dev.depotreplay.core.strategy.Strategies;
import dev.depotreplay.core.strategy.StrategyRun;
import dev.depotreplay.core.strategy.StrategyRunner;
import dev.depotreplay.core.validation.ValidationException;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;

public final class DepotReplayCli {
    private DepotReplayCli() { }

    public static void main(String[] args) {
        int status = run(args, System.out, System.err);
        if (status != 0) {
            System.exit(status);
        }
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            if (args.length == 0 || "help".equals(args[0]) || "--help".equals(args[0])) {
                printUsage(out);
                return 0;
            }
            return switch (args[0]) {
                case "validate" -> validate(args, out);
                case "run" -> runStrategy(args, out);
                case "verify" -> verify(args, out);
                case "compare" -> compare(args, out);
                case "exact" -> exact(args, out);
                case "generate" -> generate(args, out);
                case "benchmark" -> benchmark(args, out);
                case "verify-benchmark" -> verifyBenchmark(args, out);
                default -> throw new IllegalArgumentException("Unknown command: " + args[0]);
            };
        } catch (ValidationException error) {
            err.println("ERROR: " + error.getMessage());
            return 2;
        } catch (CorruptedReplayException error) {
            err.println("CORRUPTED: " + error.getMessage());
            return 3;
        } catch (IllegalArgumentException | DepotReplayException error) {
            err.println("ERROR: " + error.getMessage());
            return 2;
        }
    }

    private static int validate(String[] args, PrintStream out) {
        requireCount(args, 2, "validate <scenario.json>");
        Scenario scenario = ScenarioIo.load(Path.of(args[1]));
        out.printf("VALID scenario=%s seed=%d tasks=%d vehicles=%d%n",
                scenario.name(), scenario.seed(), scenario.tasks().size(), scenario.vehicles().size());
        out.println("scenarioHash=" + CanonicalJson.sha256(scenario));
        return 0;
    }

    private static int runStrategy(String[] args, PrintStream out) {
        requireCount(args, 4, "run <scenario.json> <strategy> <output.replay.json>");
        Scenario scenario = ScenarioIo.load(Path.of(args[1]));
        DispatchStrategy strategy = Strategies.named(args[2]);
        StrategyRun run = new StrategyRunner().run(scenario, strategy);
        ReplayService replayService = new ReplayService();
        ReplayFile replay = replayService.create(scenario, run.finalState().commandLog());
        replayService.save(Path.of(args[3]), replay);
        printRun(out, ComparisonService.toRun(strategy.id(), run.finalState()));
        out.println("replay=" + Path.of(args[3]).toAbsolutePath().normalize());
        out.println("verifiedFinalStateHash=" + replay.finalStateHash());
        return 0;
    }

    private static int verify(String[] args, PrintStream out) {
        requireCount(args, 2, "verify <replay.json>");
        ReplayService service = new ReplayService();
        ReplayVerification result = service.verify(service.load(Path.of(args[1])));
        out.printf("VERIFIED ticks=%d finalStateHash=%s%n", result.verifiedTicks(), result.finalStateHash());
        printRun(out, ComparisonService.toRun("replay", result.finalState()));
        return 0;
    }

    private static int compare(String[] args, PrintStream out) {
        if (args.length != 2 && args.length != 3) {
            throw new IllegalArgumentException("Usage: compare <scenario.json> [player.replay.json]");
        }
        Scenario scenario = ScenarioIo.load(Path.of(args[1]));
        ComparisonReport report;
        if (args.length == 3) {
            ReplayService replayService = new ReplayService();
            ReplayFile playerReplay = replayService.load(Path.of(args[2]));
            if (!CanonicalJson.sha256(scenario).equals(playerReplay.scenarioHash())) {
                throw new IllegalArgumentException("Player replay belongs to a different scenario");
            }
            ReplayVerification player = replayService.verify(playerReplay);
            report = new ComparisonService().compare(scenario, player.finalState());
        } else {
            report = new ComparisonService().compare(scenario, null);
        }
        out.println("COMPARISON scenario=" + report.scenarioName());
        report.runs().forEach(run -> printRun(out, run));
        return 0;
    }

    private static int exact(String[] args, PrintStream out) {
        if (args.length != 2 && args.length != 3) {
            throw new IllegalArgumentException("Usage: exact <scenario.json> [output.replay.json]");
        }
        Scenario scenario = ScenarioIo.load(Path.of(args[1]));
        ExactSolution solution = new ExactSolver().solve(scenario);
        printRun(out, ComparisonService.toRun(solution.solverId(), solution.finalState()));
        out.printf("exploredStates=%d scaleLimit=%s%n", solution.exploredStates(), solution.scaleLimit());
        if (args.length == 3) {
            ReplayService service = new ReplayService();
            ReplayFile replay = service.create(scenario, solution.commands());
            service.save(Path.of(args[2]), replay);
            out.println("replay=" + Path.of(args[2]).toAbsolutePath().normalize());
            out.println("verifiedFinalStateHash=" + replay.finalStateHash());
        }
        return 0;
    }

    private static int generate(String[] args, PrintStream out) {
        requireCount(args, 3, "generate <seed> <output.scenario.json>");
        long seed;
        try {
            seed = Long.parseLong(args[1]);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("seed must be a signed 64-bit integer", error);
        }
        Scenario scenario = new SeededScenarioGenerator().generate(seed);
        ScenarioIo.save(Path.of(args[2]), scenario);
        out.printf("GENERATED scenario=%s seed=%d scenarioHash=%s path=%s%n",
                scenario.name(), scenario.seed(), CanonicalJson.sha256(scenario),
                Path.of(args[2]).toAbsolutePath().normalize());
        return 0;
    }

    private static int benchmark(String[] args, PrintStream out) {
        requireCount(args, 4, "benchmark <first-seed> <scenario-count> <output.json>");
        long firstSeed = parseLong(args[1], "first-seed");
        int scenarioCount = parseInt(args[2], "scenario-count");
        BenchmarkReport report = new BenchmarkService().run(firstSeed, scenarioCount);
        Path output = Path.of(args[3]);
        CanonicalJson.write(output, report);
        String reportHash = CanonicalJson.sha256(report);
        out.printf(
                "BENCHMARK generator=%s firstSeed=%d scenarios=%d reportHash=%s%n",
                report.generatorId(), report.firstSeed(), report.scenarioCount(), reportHash
        );
        report.summaries().forEach(summary -> printBenchmarkSummary(out, summary));
        out.println("report=" + output.toAbsolutePath().normalize());
        return 0;
    }

    private static int verifyBenchmark(String[] args, PrintStream out) {
        requireCount(args, 2, "verify-benchmark <report.json>");
        Path input = Path.of(args[1]);
        BenchmarkReport report = CanonicalJson.read(input, BenchmarkReport.class);
        String reportHash = new BenchmarkService().verify(report);
        out.printf(
                "VERIFIED BENCHMARK generator=%s firstSeed=%d scenarios=%d reportHash=%s%n",
                report.generatorId(), report.firstSeed(), report.scenarioCount(), reportHash
        );
        return 0;
    }

    private static long parseLong(String value, String label) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(label + " must be a signed 64-bit integer", error);
        }
    }

    private static int parseInt(String value, String label) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(label + " must be a signed 32-bit integer", error);
        }
    }

    private static void printBenchmarkSummary(PrintStream out, BenchmarkSummary summary) {
        out.printf(
                "SUMMARY %-24s best=%d/%d total=%d distance=%d lateness=%d unserved=%d%n",
                summary.strategyId(), summary.bestScoreCount(), summary.scenarioCount(),
                summary.totalScore(), summary.totalDistance(), summary.totalLateness(),
                summary.totalUnservedTasks()
        );
    }

    private static void printRun(PrintStream out, RunComparison run) {
        out.printf(
                "RUN %-24s total=%d distance=%d(cost=%d) lateness=%d(cost=%d) unserved=%d(cost=%d)%n",
                run.label(), run.score().total(), run.score().distance(), run.score().distanceCost(),
                run.score().lateness(), run.score().latenessCost(), run.score().unservedTasks(),
                run.score().unservedCost()
        );
        run.vehicles().forEach(vehicle -> out.printf(
                "  VEHICLE %s distance=%d route=%s%n",
                vehicle.vehicleId(), vehicle.distance(), vehicle.route()
        ));
        run.tasks().forEach(task -> out.printf(
                "  TASK %s status=%s vehicle=%s pickup=%s delivery=%s deadline=%d lateness=%d%n",
                task.taskId(), task.status(), String.valueOf(task.vehicleId()),
                String.valueOf(task.pickupTick()), String.valueOf(task.deliveryTick()),
                task.deadline(), task.lateness()
        ));
        out.println("  finalStateHash=" + run.finalStateHash());
    }

    private static void requireCount(String[] args, int expected, String usage) {
        if (args.length != expected) {
            throw new IllegalArgumentException("Usage: " + usage + "; received " + Arrays.toString(args));
        }
    }

    private static void printUsage(PrintStream out) {
        out.println("DepotReplay deterministic dispatch evaluator");
        out.println("Usage:");
        out.println("  validate <scenario.json>");
        out.println("  run <scenario.json> <strategy> <output.replay.json>");
        out.println("  verify <replay.json>");
        out.println("  compare <scenario.json> [player.replay.json]");
        out.println("  exact <scenario.json> [output.replay.json]");
        out.println("  generate <seed> <output.scenario.json>");
        out.println("  benchmark <first-seed> <scenario-count> <output.json>");
        out.println("  verify-benchmark <report.json>");
    }
}
