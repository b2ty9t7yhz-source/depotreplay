# Contributing

## Development gate

Use JDK 21 and run:

```bash
./gradlew clean check --no-daemon
```

Keep code, tests, command output, and documentation in English. Add headless tests for every core rule change. Do not move simulation logic into the desktop module.

## Determinism checklist

- Never use wall-clock time in `core`.
- Give every ordering an explicit stable tie-break.
- Include new state that affects behavior in `SimulationSnapshot` and canonical hashing.
- If command semantics change, update the engine/replay version contract and ADRs.
- If exact-solver complexity changes, keep a tested, documented hard scale boundary.

## Pull requests

Describe the behavior change, its determinism impact, and the commands used to verify it. Do not report benchmark or coverage numbers unless they were measured in the submitted revision with a reproducible method.
