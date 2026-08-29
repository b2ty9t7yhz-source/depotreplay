package dev.depotreplay.core.replay;

import dev.depotreplay.core.engine.SimulationEngine;
import dev.depotreplay.core.io.CanonicalJson;
import dev.depotreplay.core.model.DeliveryTaskDefinition;
import dev.depotreplay.core.model.DispatchCommand;
import dev.depotreplay.core.model.GridDefinition;
import dev.depotreplay.core.model.PathAlgorithm;
import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ScoreWeights;
import dev.depotreplay.core.model.ServiceAction;
import dev.depotreplay.core.model.SimulationSnapshot;
import dev.depotreplay.core.model.VehicleDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortableReplayJsonTest {
    @Test
    void portableSha256MatchesPublishedVectors() {
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                PortableSha256.digestUtf8("")
        );
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                PortableSha256.digestUtf8("abc")
        );
    }

    @Test
    void reflectionFreeScenarioAndStateJsonMatchJacksonCanonicalBytes() {
        Scenario scenario = scenario();
        SimulationEngine engine = new SimulationEngine(scenario);
        SimulationSnapshot initial = engine.snapshot();
        engine.submitCommand(new DispatchCommand(0, "V1", "T1", ServiceAction.PICKUP));
        engine.submitCommand(new DispatchCommand(1, "V1", "T1", ServiceAction.DELIVER));
        SimulationSnapshot completed = engine.runUntilTerminal();

        assertEquals(CanonicalJson.string(scenario), PortableReplayJson.encodeScenario(scenario));
        assertEquals(CanonicalJson.string(initial), PortableReplayJson.encodeState(initial));
        assertEquals(CanonicalJson.string(completed), PortableReplayJson.encodeState(completed));
        assertEquals(CanonicalJson.sha256(scenario), PortableReplayJson.scenarioHash(scenario));
        assertEquals(CanonicalJson.sha256(initial), PortableReplayJson.stateHash(initial));
        assertEquals(CanonicalJson.sha256(completed), PortableReplayJson.stateHash(completed));
    }

    @Test
    void reflectionFreeReplayJsonMatchesTheExistingArtifactFormat() {
        ReplayFile replay = new ReplayService().create(scenario(), List.of(
                new DispatchCommand(0, "V1", "T1", ServiceAction.PICKUP),
                new DispatchCommand(1, "V1", "T1", ServiceAction.DELIVER)
        ));

        assertEquals(CanonicalJson.string(replay), PortableReplayJson.encode(replay));
    }

    private static Scenario scenario() {
        return new Scenario(
                1,
                "Portable βeta \"dispatch\"",
                -42,
                new GridDefinition(4, 3, List.of(new Position(3, 1))),
                List.of(
                        new VehicleDefinition("V1", new Position(0, 0), 2),
                        new VehicleDefinition("V2", new Position(3, 2), 2)
                ),
                List.of(new DeliveryTaskDefinition(
                        "T1", new Position(1, 0), new Position(2, 0), 1, 3
                )),
                new ScoreWeights(1, 5, 100),
                8,
                PathAlgorithm.ASTAR
        );
    }
}
