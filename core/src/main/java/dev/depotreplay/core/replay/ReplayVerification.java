package dev.depotreplay.core.replay;

import dev.depotreplay.core.model.SimulationSnapshot;

public record ReplayVerification(
        boolean verified,
        int verifiedTicks,
        String finalStateHash,
        SimulationSnapshot finalState
) { }
