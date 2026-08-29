package dev.depotreplay.core.replay;

import dev.depotreplay.core.engine.SimulationEngine;
import dev.depotreplay.core.engine.SimulationContract;
import dev.depotreplay.core.io.CorruptedReplayException;
import dev.depotreplay.core.io.DepotReplayException;
import dev.depotreplay.core.io.CanonicalJson;
import dev.depotreplay.core.model.DispatchCommand;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.SimulationSnapshot;
import dev.depotreplay.core.validation.ScenarioValidator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ReplayService {
    public static final String ENGINE_VERSION = SimulationContract.VERSION;

    public ReplayFile create(Scenario scenario, List<DispatchCommand> inputCommands) {
        ScenarioValidator.validate(scenario);
        List<DispatchCommand> commands = sortedCopy(inputCommands);
        SimulationEngine engine = new SimulationEngine(scenario);
        submitAll(engine, commands);
        List<TickHash> tickHashes = new ArrayList<>();
        tickHashes.add(new TickHash(0, PortableReplayJson.stateHash(engine.snapshot())));
        while (!engine.isTerminal()) {
            SimulationSnapshot state = engine.advanceOneTick();
            tickHashes.add(new TickHash(state.tick(), PortableReplayJson.stateHash(state)));
        }
        String finalStateHash = tickHashes.getLast().stateHash();
        return new ReplayFile(
                1,
                ENGINE_VERSION,
                scenario,
                PortableReplayJson.scenarioHash(scenario),
                commands,
                tickHashes,
                finalStateHash
        );
    }

    public ReplayVerification verify(ReplayFile replay) {
        validateEnvelope(replay);
        String actualScenarioHash = PortableReplayJson.scenarioHash(replay.scenario());
        if (!actualScenarioHash.equals(replay.scenarioHash())) {
            throw new CorruptedReplayException(
                    "Scenario hash mismatch: expected " + replay.scenarioHash()
                            + " but was " + actualScenarioHash
            );
        }

        SimulationEngine engine;
        try {
            engine = new SimulationEngine(replay.scenario());
            submitAll(engine, replay.commands());
        } catch (IllegalArgumentException error) {
            throw new CorruptedReplayException("Replay commands or scenario are invalid", error);
        }

        int hashIndex;
        try {
            verifyTick(replay.tickHashes(), 0, engine.snapshot());
            hashIndex = 1;
            while (!engine.isTerminal()) {
                SimulationSnapshot state = engine.advanceOneTick();
                verifyTick(replay.tickHashes(), hashIndex, state);
                hashIndex++;
            }
        } catch (CorruptedReplayException error) {
            throw error;
        } catch (RuntimeException error) {
            throw new CorruptedReplayException("Replay diverged while applying command history", error);
        }
        if (hashIndex != replay.tickHashes().size()) {
            throw new CorruptedReplayException("Replay contains unexpected trailing tick hashes");
        }
        String actualFinalHash = PortableReplayJson.stateHash(engine.snapshot());
        if (!actualFinalHash.equals(replay.finalStateHash())) {
            throw new CorruptedReplayException(
                    "Final state hash mismatch: expected " + replay.finalStateHash()
                            + " but was " + actualFinalHash
            );
        }
        return new ReplayVerification(true, hashIndex, actualFinalHash, engine.snapshot());
    }

    public ReplayFile load(Path path) {
        try {
            return CanonicalJson.read(path, ReplayFile.class);
        } catch (DepotReplayException error) {
            throw new CorruptedReplayException("Could not parse replay file " + path, error);
        }
    }

    public void save(Path path, ReplayFile replay) {
        verify(replay);
        CanonicalJson.write(path, replay);
    }

    private static List<DispatchCommand> sortedCopy(List<DispatchCommand> commands) {
        if (commands == null) {
            throw new IllegalArgumentException("commands must not be null");
        }
        return commands.stream()
                .sorted(Comparator.comparingInt(DispatchCommand::tick)
                        .thenComparing(DispatchCommand::vehicleId)
                        .thenComparing(DispatchCommand::taskId)
                        .thenComparing(DispatchCommand::action))
                .toList();
    }

    private static void submitAll(SimulationEngine engine, List<DispatchCommand> commands) {
        for (DispatchCommand command : commands) {
            engine.submitCommand(command);
        }
    }

    private static void validateEnvelope(ReplayFile replay) {
        if (replay == null) {
            throw new CorruptedReplayException("Replay must not be null");
        }
        if (replay.schemaVersion() != 1) {
            throw new CorruptedReplayException("Unsupported replay schemaVersion: " + replay.schemaVersion());
        }
        if (!ENGINE_VERSION.equals(replay.engineVersion())) {
            throw new CorruptedReplayException("Unsupported engineVersion: " + replay.engineVersion());
        }
        if (replay.scenario() == null || replay.scenarioHash() == null
                || replay.commands() == null || replay.tickHashes() == null
                || replay.tickHashes().isEmpty() || replay.finalStateHash() == null) {
            throw new CorruptedReplayException("Replay is missing required fields");
        }
    }

    private static void verifyTick(List<TickHash> expectedHashes, int index, SimulationSnapshot state) {
        if (index >= expectedHashes.size()) {
            throw new CorruptedReplayException("Replay is missing hash for tick " + state.tick());
        }
        TickHash expected = expectedHashes.get(index);
        if (expected.tick() != state.tick()) {
            throw new CorruptedReplayException(
                    "Replay tick mismatch at index " + index + ": expected "
                            + expected.tick() + " but simulation reached " + state.tick()
            );
        }
        String actualHash = PortableReplayJson.stateHash(state);
        if (!actualHash.equals(expected.stateHash())) {
            throw new CorruptedReplayException(
                    "State hash mismatch at tick " + state.tick() + ": expected "
                            + expected.stateHash() + " but was " + actualHash
            );
        }
    }
}
