package dev.depotreplay.core.compare;

import dev.depotreplay.core.model.Position;

import java.util.List;

public record VehicleComparison(String vehicleId, int distance, List<Position> route) {
    public VehicleComparison {
        route = List.copyOf(route);
    }
}
