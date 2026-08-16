package dev.depotreplay.cli;

import dev.depotreplay.core.io.ScenarioIo;
import dev.depotreplay.core.model.DeliveryTaskDefinition;
import dev.depotreplay.core.model.GridDefinition;
import dev.depotreplay.core.model.PathAlgorithm;
import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ScoreWeights;
import dev.depotreplay.core.model.VehicleDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepotReplayCliTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void runsAndVerifiesAReplayFromTheCommandLineContract() {
        Path scenarioPath = temporaryDirectory.resolve("scenario.json");
        Path replayPath = temporaryDirectory.resolve("run.replay.json");
        ScenarioIo.save(scenarioPath, scenario());
        Captured run = invoke("run", scenarioPath.toString(), "nearest-task", replayPath.toString());
        Captured verify = invoke("verify", replayPath.toString());

        assertEquals(0, run.status());
        assertTrue(run.out().contains("verifiedFinalStateHash="));
        assertEquals(0, verify.status());
        assertTrue(verify.out().contains("VERIFIED ticks="));
    }

    @Test
    void returnsANonzeroStatusForBadInput() {
        Captured captured = invoke("unknown");
        assertEquals(2, captured.status());
        assertTrue(captured.err().contains("Unknown command"));
    }

    private static Captured invoke(String... args) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int status = DepotReplayCli.run(
                args,
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(errors, true, StandardCharsets.UTF_8)
        );
        return new Captured(
                status,
                output.toString(StandardCharsets.UTF_8),
                errors.toString(StandardCharsets.UTF_8)
        );
    }

    private static Scenario scenario() {
        return new Scenario(
                1, "CLI fixture", 11,
                new GridDefinition(4, 3, List.of()),
                List.of(
                        new VehicleDefinition("V1", new Position(0, 0), 2),
                        new VehicleDefinition("V2", new Position(3, 2), 2)
                ),
                List.of(new DeliveryTaskDefinition(
                        "T1", new Position(1, 0), new Position(2, 0), 1, 4
                )),
                new ScoreWeights(1, 5, 100), 8, PathAlgorithm.ASTAR
        );
    }

    private record Captured(int status, String out, String err) { }
}
