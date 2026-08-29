package dev.depotreplay.web;

import dev.depotreplay.core.io.CorruptedReplayException;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.replay.ReplayFile;
import dev.depotreplay.core.replay.ReplayService;
import dev.depotreplay.core.scenario.BuiltinScenarios;
import dev.depotreplay.core.strategy.NearestTaskStrategy;
import dev.depotreplay.core.strategy.StrategyRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserReplayJsonTest {
    private final ReplayService replayService = new ReplayService();

    @Test
    void roundTripsAndVerifiesARepresentativeReplay() {
        ReplayFile expected = representativeReplay(BuiltinScenarios.cityGrid());

        ReplayFile decoded = BrowserReplayJson.decode(BrowserReplayJson.encode(expected));

        assertEquals(expected, decoded);
        assertEquals(expected.finalStateHash(), replayService.verify(decoded).finalStateHash());
    }

    @Test
    void preservesLongSeedAndEscapedUnicodeAtTheFormatBoundary() {
        Scenario original = BuiltinScenarios.cityGrid();
        Scenario boundary = new Scenario(
                original.schemaVersion(),
                "Harbor \"β\"\nDispatch",
                Long.MIN_VALUE,
                original.grid(),
                original.vehicles(),
                original.tasks(),
                original.scoreWeights(),
                original.maxTicks(),
                original.pathAlgorithm()
        );
        ReplayFile expected = representativeReplay(boundary);

        ReplayFile decoded = BrowserReplayJson.decode(BrowserReplayJson.encode(expected));

        assertEquals(Long.MIN_VALUE, decoded.scenario().seed());
        assertEquals(boundary.name(), decoded.scenario().name());
        assertEquals(expected.finalStateHash(), replayService.verify(decoded).finalStateHash());
    }

    @Test
    void rejectsMalformedMissingUnknownDuplicateAndNonIntegerFields() {
        ReplayFile replay = representativeReplay(BuiltinScenarios.cityGrid());
        String encoded = BrowserReplayJson.encode(replay);

        assertThrows(CorruptedReplayException.class, () -> BrowserReplayJson.decode("{"));
        assertThrows(
                CorruptedReplayException.class,
                () -> BrowserReplayJson.decode(encoded.replaceFirst("\"engineVersion\":\"[^\"]+\",", ""))
        );
        assertThrows(
                CorruptedReplayException.class,
                () -> BrowserReplayJson.decode(encoded.replaceFirst("\\{", "{\"unexpected\":true,"))
        );
        assertThrows(
                CorruptedReplayException.class,
                () -> BrowserReplayJson.decode(encoded.replaceFirst("\\{", "{\"schemaVersion\":1,"))
        );
        assertThrows(
                CorruptedReplayException.class,
                () -> BrowserReplayJson.decode(encoded.replaceFirst("\"schemaVersion\":1", "\"schemaVersion\":1.5"))
        );
    }

    @Test
    void parsesButVerificationDetectsAChangedTickHash() {
        ReplayFile replay = representativeReplay(BuiltinScenarios.cityGrid());
        String firstHash = replay.tickHashes().getFirst().stateHash();
        String corrupted = BrowserReplayJson.encode(replay).replaceFirst(firstHash, "0".repeat(64));

        ReplayFile decoded = BrowserReplayJson.decode(corrupted);
        CorruptedReplayException error = assertThrows(
                CorruptedReplayException.class,
                () -> replayService.verify(decoded)
        );

        assertTrue(error.getMessage().contains("tick 0"));
    }

    private ReplayFile representativeReplay(Scenario scenario) {
        return replayService.create(
                scenario,
                new StrategyRunner().run(scenario, new NearestTaskStrategy()).finalState().commandLog()
        );
    }
}
