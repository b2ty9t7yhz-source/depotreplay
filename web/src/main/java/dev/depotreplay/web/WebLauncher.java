package dev.depotreplay.web;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import dev.depotreplay.core.scenario.BuiltinScenarios;
import dev.depotreplay.game.DepotReplayGame;
import dev.depotreplay.game.NoFrameCapture;

public final class WebLauncher {
    private WebLauncher() { }

    public static void main(String[] args) {
        WebApplicationConfiguration configuration = new WebApplicationConfiguration();
        configuration.width = 1280;
        configuration.height = 800;
        new WebApplication(
                new DepotReplayGame(
                        BuiltinScenarios.cityGrid(),
                        new BrowserGamePersistence(),
                        new NoFrameCapture()
                ),
                configuration
        );
    }
}
