package dev.depotreplay.core.exact;

import dev.depotreplay.core.model.DispatchCommand;
import dev.depotreplay.core.model.SimulationSnapshot;

import java.util.List;

public record ExactSolution(
        String solverId,
        String scaleLimit,
        long exploredStates,
        List<DispatchCommand> commands,
        SimulationSnapshot finalState
) {
    public ExactSolution {
        commands = List.copyOf(commands);
    }
}
