package dev.depotreplay.core.model;

/** Non-negative weights used by the documented score formula. */
public record ScoreWeights(int distance, int lateness, int unserved) { }
