package dev.depotreplay.web;

import dev.depotreplay.core.engine.SimulationEngine;
import dev.depotreplay.core.io.CorruptedReplayException;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.replay.ReplayFile;
import dev.depotreplay.core.replay.ReplayService;
import dev.depotreplay.core.replay.ReplayVerification;
import dev.depotreplay.game.GamePersistence;
import dev.depotreplay.game.PersistenceResult;
import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Storage;

final class BrowserGamePersistence implements GamePersistence {
    static final String IMPORTED_REPLAY_KEY = "depotreplay.replay.import";
    static final String SAVED_REPLAY_KEY = "depotreplay.replay.saved";
    private boolean readyMarked;

    @Override
    public boolean available() {
        if (!readyMarked) {
            markReady();
            readyMarked = true;
        }
        return true;
    }

    @Override
    public PersistenceResult save(Scenario scenario, SimulationEngine engine) {
        ReplayFile replay = verifiedReplay(scenario, engine);
        Storage.getLocalStorage().setItem(SAVED_REPLAY_KEY, BrowserReplayJson.encode(replay));
        String message = "Saved verified browser replay: " + shortHash(replay) + "...";
        announce(message);
        return new PersistenceResult(engine, message);
    }

    @Override
    public PersistenceResult load(Scenario scenario, SimulationEngine engine) {
        try {
            Storage storage = Storage.getLocalStorage();
            String encoded = storage.getItem(IMPORTED_REPLAY_KEY);
            String source = "selected file";
            if (encoded == null) {
                encoded = storage.getItem(SAVED_REPLAY_KEY);
                source = "browser storage";
            }
            if (encoded == null) {
                throw new CorruptedReplayException("No replay selected or saved in this browser");
            }

            ReplayFile replay = BrowserReplayJson.decode(encoded);
            ReplayVerification verification = new ReplayService().verify(replay);
            if (!scenario.equals(replay.scenario())) {
                throw new CorruptedReplayException("Replay belongs to a different scenario");
            }

            SimulationEngine reviewEngine = new SimulationEngine(scenario);
            replay.commands().forEach(reviewEngine::submitCommand);
            String message = "Verified " + source + ": " + verification.verifiedTicks()
                    + " tick hashes; use SPACE to replay.";
            announce(message);
            return new PersistenceResult(
                    reviewEngine,
                    message,
                    true,
                    "VERIFIED REPLAY REVIEW"
            );
        } catch (RuntimeException error) {
            announce("Replay rejected: " + error.getMessage());
            throw error;
        }
    }

    @Override
    public PersistenceResult exportReplay(Scenario scenario, SimulationEngine engine) {
        ReplayFile replay = verifiedReplay(scenario, engine);
        String filename = "depotreplay-" + scenario.seed() + "-tick-"
                + engine.currentTick() + ".replay.json";
        download(filename, BrowserReplayJson.encode(replay));
        String message = "Downloaded verified replay: " + shortHash(replay) + "...";
        announce(message);
        return new PersistenceResult(engine, message);
    }

    private static ReplayFile verifiedReplay(Scenario scenario, SimulationEngine engine) {
        ReplayService service = new ReplayService();
        ReplayFile replay = service.create(scenario, engine.snapshot().commandLog());
        service.verify(replay);
        return replay;
    }

    private static String shortHash(ReplayFile replay) {
        return replay.finalStateHash().substring(0, 12);
    }

    @JSBody(
            params = {"filename", "content"},
            script = "var blob = new Blob([content], {type: 'application/json'});"
                    + "var url = URL.createObjectURL(blob);"
                    + "var link = document.createElement('a');"
                    + "link.href = url; link.download = filename; link.click();"
                    + "setTimeout(function() { URL.revokeObjectURL(url); }, 0);"
    )
    private static native void download(String filename, String content);

    @JSBody(
            params = {"message"},
            script = "var status = document.getElementById('replay-status');"
                    + "if (status) { status.textContent = message; }"
    )
    private static native void announce(String message);

    @JSBody(
            script = "var canvas = document.getElementById('canvas');"
                    + "if (canvas) { canvas.dataset.gameReady = 'true'; }"
    )
    private static native void markReady();
}
