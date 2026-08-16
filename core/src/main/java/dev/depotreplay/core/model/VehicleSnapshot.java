package dev.depotreplay.core.model;

import java.util.List;

public record VehicleSnapshot(
        String id,
        Position position,
        int capacity,
        int load,
        List<String> cargo,
        int distanceTraveled,
        List<Position> remainingRoute,
        String activeTaskId,
        ServiceAction activeAction,
        List<Position> routeTrace
) {
    public VehicleSnapshot {
        cargo = List.copyOf(cargo);
        remainingRoute = List.copyOf(remainingRoute);
        routeTrace = List.copyOf(routeTrace);
    }
}
