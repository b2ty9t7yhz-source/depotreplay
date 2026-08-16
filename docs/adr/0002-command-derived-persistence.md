# ADR 0002: Derive persisted state from commands

- Status: Accepted
- Date: 2026-08-13

## Context

Serializing private mutable runtime objects would make files tightly coupled to implementation details and would not prove that a replay follows the same transitions as the original run.

## Decision

Treat the scenario plus ordered `DispatchCommand` values as the source of truth. Save files reconstruct to a specified tick. Replay files reconstruct through terminal state and include a per-tick canonical SHA-256 sequence. Persisted hashes are checked before any reconstructed state is returned as verified.

## Consequences

- Corrupted commands and divergent transitions are detected at the first affected tick.
- Persistence remains independent of private runtime classes.
- Loading costs a deterministic replay rather than a raw object graph read.
- SHA-256 detects changes but does not authenticate authorship; signed replays remain out of scope.
