# ADR 0001: Keep simulation rules in a headless deterministic core

- Status: Accepted
- Date: 2026-08-13

## Context

The game needs interactive rendering, but strategy comparison and replay verification must not depend on frame rate, input timing, or OpenGL availability. Putting rules in libGDX screen classes would make headless tests incomplete and allow visual and algorithm runs to diverge.

## Decision

Use separate Gradle modules. `core` owns models, validation, pathfinding, fixed-tick state transitions, scoring, strategies, exact solving, canonical I/O, save/load, and replay verification. `desktop` depends on `core` and translates keyboard input to commands while rendering immutable snapshots.

The core must not read wall-clock time or depend on libGDX.

## Consequences

- CI can test every rule without a display.
- Player and algorithm results share one engine and score implementation.
- Desktop code contains some presentation duplication, such as colors and text layout, but no game-rule duplication.
- New front ends can reuse the core API.
