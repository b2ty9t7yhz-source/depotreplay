# ADR 0006: Portable browser replay verification

- Status: Accepted
- Date: 2026-08-28

## Context

Replay verification originally used canonical JSON and SHA-256 through JVM-oriented persistence code. The reflection-disabled TeaVM build could render deterministic runs but could not safely reach the reflective Jackson object mapper. Leaving verification on desktop would make the public browser demo unable to exchange or inspect the project's strongest correctness artifact.

## Decision

Keep `ReplayFile` and `ReplayService` as the single semantic contract, and add two narrow portability components:

1. `PortableReplayJson` writes the exact existing canonical scenario, snapshot, and replay representation without reflection and computes SHA-256 with platform-independent Java code.
2. `BrowserReplayJson` strictly decodes only replay schema version 1 with libGDX's TeaVM-compatible JSON tree parser. Semantic validation and all scenario, command, tick, and final-state hashes remain the responsibility of `ReplayService`.

The web adapter may expose local storage, file selection, and download capabilities. It must not return a review engine until verification succeeds. A loaded browser replay starts at tick 0 with its commands queued and advances through the existing libGDX renderer.

## Consequences

- JVM and browser replays remain one format; there is no browser-only artifact.
- Compatibility tests must prove byte-for-byte equality with the Jackson canonical representation and published SHA-256 vectors.
- The browser decoder duplicates schema field mapping, so schema changes require explicit updates and tests in both codecs.
- Strict unknown-field rejection favors reproducibility over forward-compatible best-effort parsing.
- Local storage is convenient but not a durable checkpoint. Downloaded replay files are the portable boundary.
- Replay hashes detect changes, not authorship; signatures remain outside scope.
