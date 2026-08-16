package dev.depotreplay.core.model;

/** Raw score inputs and their weighted costs. Lower total cost is better. */
public record ScoreBreakdown(
        int distance,
        int lateness,
        int unservedTasks,
        long distanceCost,
        long latenessCost,
        long unservedCost,
        long total
) { }
