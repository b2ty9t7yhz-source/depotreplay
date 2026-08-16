package dev.depotreplay.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import java.nio.file.Path;

public final class DesktopLauncher {
    private DesktopLauncher() { }

    public static void main(String[] args) {
        LaunchOptions options = LaunchOptions.parse(args);
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("DepotReplay");
        configuration.setWindowedMode(1280, 800);
        configuration.setResizable(false);
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);
        new Lwjgl3Application(
                new DepotReplayGame(options.scenarioPath(), options.screenshotPath()),
                configuration
        );
    }

    record LaunchOptions(Path scenarioPath, Path screenshotPath) {
        static LaunchOptions parse(String[] args) {
            Path scenario = Path.of("examples/city-grid.json");
            Path screenshot = null;
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
                    default -> throw new IllegalArgumentException("Unknown desktop option: " + args[index]);
                }
            }
            return new LaunchOptions(scenario, screenshot);
        }

        private static void ensureValue(String[] args, int index, String option) {
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException(option + " requires a path");
            }
        }
    }
}
