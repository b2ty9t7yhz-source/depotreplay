package dev.depotreplay.desktop;

import java.nio.file.Path;
import java.util.Locale;

final class UserDataDirectory {
    private UserDataDirectory() { }

    static Path resolve() {
        if (!Boolean.getBoolean("depotreplay.packaged")) {
            return Path.of("build");
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home");
        if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support", "DepotReplay");
        }
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return appData == null || appData.isBlank()
                    ? Path.of(home, "AppData", "Roaming", "DepotReplay")
                    : Path.of(appData, "DepotReplay");
        }
        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        return xdgDataHome == null || xdgDataHome.isBlank()
                ? Path.of(home, ".local", "share", "depotreplay")
                : Path.of(xdgDataHome, "depotreplay");
    }
}
