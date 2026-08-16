package dev.depotreplay.core.compare;

import dev.depotreplay.core.model.TaskStatus;

public record TaskCompletion(
        String taskId,
        TaskStatus status,
        String vehicleId,
        Integer pickupTick,
        Integer deliveryTick,
        int deadline,
        int lateness
) { }
