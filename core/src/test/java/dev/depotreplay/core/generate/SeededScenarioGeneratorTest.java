package dev.depotreplay.core.generate;

import dev.depotreplay.core.io.CanonicalJson;
import dev.depotreplay.core.model.Scenario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SeededScenarioGeneratorTest {
    @Test
    void theSameSeedProducesTheSameCanonicalScenario() {
        SeededScenarioGenerator generator = new SeededScenarioGenerator();
        Scenario first = generator.generate(12345);
        Scenario second = generator.generate(12345);
        assertEquals(first, second);
        assertEquals(CanonicalJson.sha256(first), CanonicalJson.sha256(second));
    }

    @Test
    void differentSeedsChangeTheGeneratedScenario() {
        SeededScenarioGenerator generator = new SeededScenarioGenerator();
        assertNotEquals(
                CanonicalJson.sha256(generator.generate(1)),
                CanonicalJson.sha256(generator.generate(2))
        );
    }
}
