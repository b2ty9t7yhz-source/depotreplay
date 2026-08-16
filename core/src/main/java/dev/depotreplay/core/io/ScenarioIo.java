package dev.depotreplay.core.io;

import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.validation.ScenarioValidator;

import java.nio.file.Path;

public final class ScenarioIo {
    private ScenarioIo() { }

    public static Scenario load(Path path) {
        Scenario scenario = CanonicalJson.read(path, Scenario.class);
        ScenarioValidator.validate(scenario);
        return scenario;
    }

    public static void save(Path path, Scenario scenario) {
        ScenarioValidator.validate(scenario);
        CanonicalJson.write(path, scenario);
    }
}
