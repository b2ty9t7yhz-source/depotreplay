# ADR 0003: Bound the exhaustive solver explicitly

- Status: Accepted
- Date: 2026-08-13

## Context

An exact reference is valuable for evaluating heuristics on small scenarios, but pickup-and-delivery ordering and vehicle assignment grow exponentially. An unbounded `exact` command would invite misleading scalability expectations and poor failure behavior.

## Decision

Use exhaustive event-order search with memoization and a hard public input gate: at most 5 tasks, exactly 2 vehicles, at most 400 grid cells, and at most 200 ticks. Include the boundary in API output, CLI output, documentation, and tests. Replay the selected plan through the standard engine and reject it if the resulting score differs from the search score.

## Consequences

- Small scenarios have an exact score reference under the modeled rules.
- The solver cannot be presented as a general vehicle-routing optimizer.
- Larger scenarios must use player decisions or deterministic baselines.
- Future solver work can introduce a different algorithm and identity without silently changing `exact-small` semantics.
