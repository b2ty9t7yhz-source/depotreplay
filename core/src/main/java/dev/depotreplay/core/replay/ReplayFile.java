package dev.depotreplay.core.replay;

import dev.depotreplay.core.model.DispatchCommand;
import dev.depotreplay.core.model.Scenario;

import java.util.List;

/** Self-contained deterministic replay with a hash chain over each state. */
public record ReplayFile(
        int schemaVersion,
        String engineVersion,
        Scenario scenario,
        String scenarioHash,
        List<DispatchCommand> commands,
        List<TickHash> tickHashes,
        String finalStateHash
) {
    public ReplayFile {
        commands = List.copyOf(commands);
        tickHashes = List.copyOf(tickHashes);
    }
}
