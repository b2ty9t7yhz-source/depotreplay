package dev.depotreplay.core.scenario;

import dev.depotreplay.core.io.CanonicalJson;
import dev.depotreplay.core.io.ScenarioIo;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuiltinScenariosTest {
    @Test
    void builtInCityGridMatchesTheCheckedInScenario() {
        assertEquals(
                CanonicalJson.sha256(ScenarioIo.load(Path.of("../examples/city-grid.json"))),
                CanonicalJson.sha256(BuiltinScenarios.cityGrid())
        );
    }
}
