package dev.depotreplay.core.model;

/** A deterministic task-level command applied at the start of a simulation tick. */
public record DispatchCommand(int tick, String vehicleId, String taskId, ServiceAction action) { }
