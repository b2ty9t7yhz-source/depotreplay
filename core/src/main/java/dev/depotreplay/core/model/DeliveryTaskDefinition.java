package dev.depotreplay.core.model;

/** A pickup-and-delivery request with an inclusive completion deadline. */
public record DeliveryTaskDefinition(
        String id,
        Position pickup,
        Position delivery,
        int demand,
        int deadline
) { }
