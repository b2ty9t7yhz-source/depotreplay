package dev.depotreplay.core.save;

import dev.depotreplay.core.model.DispatchCommand;
import dev.depotreplay.core.model.Scenario;

import java.util.List;

/** A resumable checkpoint reconstructed from its scenario and command history. */
public record SaveFile(
        int schemaVersion,
        Scenario scenario,
        String scenarioHash,
        int savedAtTick,
        List<DispatchCommand> commands,
        String stateHash
) {
    public SaveFile {
        commands = List.copyOf(commands);
    }
}
