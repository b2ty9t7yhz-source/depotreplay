# DepotReplay

**A deterministic dispatch strategy game with counterfactual evaluation.**

DepotReplay is a Java 21/libGDX desktop game and headless simulation laboratory. A player dispatches two vehicles across a four-neighbor grid, picking up and delivering capacity-constrained tasks before their deadlines. The same scenario can be rerun with three transparent baselines or a bounded exact solver, so comparisons use identical rules rather than separate implementations.

![DepotReplay gameplay and comparison dashboard](docs/images/depotreplay-gameplay.png)

## Why this project exists

Dispatch games are easy to make visually plausible and hard to make reproducible. DepotReplay treats reproducibility as part of the game contract:

- fixed logical ticks instead of wall-clock time;
- explicit 64-bit scenario seeds and a reproducible seeded generator;
- a command log as the only player input history;
- canonical JSON serialization and SHA-256 state hashes;
- per-tick replay verification, including corrupted replay detection;
- one headless core shared by player, baseline, exact-solver, save/load, and replay flows.

The project does **not** claim real-world vehicle-routing performance. It is a deterministic strategy game and evaluation environment over synthetic grid scenarios.

## Requirements

- JDK 21
- macOS, Linux, or Windows with a desktop OpenGL environment for the game

No system Gradle installation is required; the repository includes the Gradle Wrapper.

## Build and test

From the repository root:

```bash
./gradlew clean check --no-daemon
```

This compiles both modules with Java 21 and `-Xlint:all -Werror`, then runs the headless JUnit 5 suite and generates the JaCoCo report at `core/build/reports/jacoco/test/html/index.html`.

Direct and transitive dependency versions are locked per module. Gradle verifies downloaded artifacts against checked-in SHA-256 metadata in strict mode, and the Wrapper distribution has its own pinned SHA-256 checksum.

## Play the desktop game

```bash
./gradlew :desktop:run --no-daemon
```

Controls:

| Key | Action |
| --- | --- |
| `1` / `2` | Select a vehicle |
| `Up` / `Down` | Select a task |
| `P` / `D` | Queue pickup or delivery for the current tick |
| `Space` | Advance exactly one logical tick |
| `A` | Auto-dispatch idle vehicles with nearest-task, then advance one tick |
| `R` | Reset the scenario |
| `S` / `L` | Save or load a hash-verified checkpoint |
| `E` | Export and verify a replay under `build/` |
| `Esc` | Exit |

Run a different scenario:

```bash
./gradlew :desktop:run --args='--scenario examples/exact-mini.json' --no-daemon
```

Regenerate the checked-in screenshot from a real rendered frame:

```bash
./gradlew :desktop:run --args='--scenario examples/city-grid.json --screenshot docs/images/depotreplay-gameplay.png' --no-daemon
```

## Headless examples

Validate an input and print its canonical scenario hash:

```bash
./gradlew :core:run --args='validate examples/city-grid.json' --no-daemon
```

Run a baseline, persist a replay, and verify every state hash:

```bash
./gradlew :core:run --args='run examples/city-grid.json nearest-task build/city.replay.json' --no-daemon
./gradlew :core:run --args='verify build/city.replay.json' --no-daemon
```

Compare score components, vehicle routes, pickup ticks, and delivery ticks:

```bash
./gradlew :core:run --args='compare examples/city-grid.json build/city.replay.json' --no-daemon
```

Solve the deliberately small exact example and write a verified replay:

```bash
./gradlew :core:run --args='exact examples/exact-mini.json build/exact-mini.replay.json' --no-daemon
```

Generate the same canonical scenario whenever the seed is the same:

```bash
./gradlew :core:run --args='generate 12345 build/seeded-12345.json' --no-daemon
```

All CLI commands return a nonzero status for invalid input or I/O errors. Replay corruption has a distinct exit status and a `CORRUPTED:` diagnostic.

## Game rules

- The map is a rectangular 2D grid with blocked cells and unit-cost, four-neighbor movement.
- Exactly two vehicles start at fixed traversable cells. Each has an integer capacity.
- A task has a pickup, delivery, demand, and inclusive completion deadline.
- A vehicle may carry multiple tasks while total demand remains within capacity.
- One route cell is traversed per tick. Service at the current cell consumes one tick.
- A run ends when every task is delivered or `maxTicks` is reached.

Invalid IDs, dimensions, duplicate obstacles, blocked endpoints, disconnected required cells, impossible demands, bad deadlines, unsupported schemas, and malformed commands are rejected with actionable messages.

## Routing and dispatch strategies

Both Dijkstra and A* use the same deterministic neighbor and tie ordering. Because every edge costs one, both return a shortest path; A* uses Manhattan distance as its heuristic.

The baselines are intentionally understandable:

- **nearest-task** chooses the closest actionable pickup or onboard delivery;
- **earliest-deadline-first** prioritizes the smallest deadline, then distance;
- **capacity-aware** prefers high-demand pickups that fit, but delivers onboard work when its remaining deadline slack cannot absorb the nearest pickup detour.

They are comparison baselines, not claims of optimality.

### Exact solver boundary

`exact-small` exhaustively searches feasible pickup/delivery event orders and vehicle assignments, then replays its winning command sequence through the normal engine. It accepts **at most 5 tasks, exactly 2 vehicles, 400 grid cells, and 200 ticks**. Inputs outside any limit fail fast. This explicit cap keeps the exponential search honest and prevents the CLI from implying production-scale optimization.

## Score

Lower cost is better:

```text
total = distanceWeight * totalVehicleMoves
      + latenessWeight * sum(max(0, deliveryTick - deadline))
      + unservedWeight * count(tasks not delivered)
```

The UI and CLI expose raw distance, lateness, and unserved counts plus every weighted component. No hidden bonus or random tie-break affects the score.

## Determinism, save/load, and replay verification

A canonical state contains the scenario identity, seed, tick, grid, sorted vehicle/task state, route traces, score, and applied command log. Canonical JSON uses stable property ordering, stable collection ordering, UTF-8, and no insignificant whitespace before SHA-256 hashing.

A replay is self-contained and records:

1. the scenario and its hash;
2. the ordered command log;
3. the canonical state hash for tick 0 and every subsequent tick;
4. the expected final state hash.

Verification reconstructs the simulation rather than trusting stored state. It stops at the first scenario, command, tick, or hash mismatch. Save files use the same principle: scenario plus applied command history are replayed to `savedAtTick`, and the reconstructed state must match the stored hash before the engine is returned.

See [Replay and save format](docs/replay-and-save.md) for the precise contracts.

## Architecture

```text
desktop (libGDX input + rendering)
              |
              v
core (engine, model, routing, strategies, exact solver, scoring, I/O)
              |
              v
headless JUnit tests + CLI + canonical JSON artifacts
```

The `core` module has no libGDX dependency and never reads wall-clock time, input devices, or rendering state. The desktop module translates keys into `DispatchCommand` values and renders immutable snapshots.

More detail:

- [Architecture](docs/architecture.md)
- [Scenario format](docs/scenario-format.md)
- [Test strategy](docs/test-strategy.md)
- [ADR 0001: headless deterministic core](docs/adr/0001-headless-deterministic-core.md)
- [ADR 0002: command-derived persistence](docs/adr/0002-command-derived-persistence.md)
- [ADR 0003: bounded exhaustive solver](docs/adr/0003-bounded-exact-solver.md)

## Repository layout

```text
core/       Java simulation, CLI, persistence, strategies, and headless tests
desktop/    libGDX desktop UI and screenshot capture
examples/   validated synthetic scenarios
docs/       architecture, ADRs, formats, test strategy, and screenshot
.github/    continuous integration workflow
```

## Continuous integration

GitHub Actions uses Temurin Java 21 and the checked-in Gradle Wrapper. CI runs a clean `check`, uploads JUnit XML on failure, and uploads the JaCoCo HTML/XML reports after every run. Desktop code is compiled; UI behavior is kept thin while all game rules are covered headlessly.

## License

DepotReplay is available under the [MIT License](LICENSE).
