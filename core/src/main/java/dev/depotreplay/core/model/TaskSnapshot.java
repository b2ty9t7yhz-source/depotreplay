package dev.depotreplay.core.model;

public record TaskSnapshot(
        String id,
        Position pickup,
        Position delivery,
        int demand,
        int deadline,
        TaskStatus status,
        String vehicleId,
        Integer pickupTick,
        Integer deliveryTick
) { }
