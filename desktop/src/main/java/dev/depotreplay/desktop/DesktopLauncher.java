package dev.depotreplay.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import dev.depotreplay.core.io.ScenarioIo;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.scenario.BuiltinScenarios;
import dev.depotreplay.game.DepotReplayGame;
import dev.depotreplay.game.FrameCapture;
import dev.depotreplay.game.NoFrameCapture;

import java.nio.file.Path;

public final class DesktopLauncher {
    private DesktopLauncher() { }

    public static void main(String[] args) {
        LaunchOptions options = LaunchOptions.parse(args);
        Scenario scenario = options.scenarioPath() == null
                ? BuiltinScenarios.cityGrid()
                : ScenarioIo.load(options.scenarioPath());
        FrameCapture frameCapture = options.screenshotPath() == null
                ? new NoFrameCapture()
                : new PngFrameCapture(options.screenshotPath());
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("DepotReplay");
        configuration.setWindowedMode(1280, 800);
        configuration.setResizable(false);
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);
        new Lwjgl3Application(
                new DepotReplayGame(
                        scenario,
                        new FileGamePersistence(options.dataDirectory()),
                        frameCapture
                ),
                configuration
        );
    }

    record LaunchOptions(Path scenarioPath, Path screenshotPath, Path dataDirectory) {
        static LaunchOptions parse(String[] args) {
            Path scenario = null;
            Path screenshot = null;
            Path dataDirectory = UserDataDirectory.resolve();
            for (int index = 0; index < args.length; index++) {
                switch (args[index]) {
                    case "--scenario" -> {
                        ensureValue(args, index, "--scenario");
                        scenario = Path.of(args[++index]);
                    }
                    case "--screenshot" -> {
                        ensureValue(args, index, "--screenshot");
                        screenshot = Path.of(args[++index]);
                    }
                    case "--data-dir" -> {
                        ensureValue(args, index, "--data-dir");
                        dataDirectory = Path.of(args[++index]);
                    }
                    default -> throw new IllegalArgumentException("Unknown desktop option: " + args[index]);
                }
            }
            return new LaunchOptions(scenario, screenshot, dataDirectory);
        }

        private static void ensureValue(String[] args, int index, String option) {
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException(option + " requires a path");
            }
        }
    }
}
