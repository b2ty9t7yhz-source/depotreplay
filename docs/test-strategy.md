# Test strategy

All domain tests run headlessly in `core`; they do not initialize libGDX or OpenGL.

## Covered behaviors

- Dijkstra/A* shortest-path agreement, deterministic tie behavior, blocked endpoints, and disconnected targets;
- aggregated scenario validation and accepted valid inputs;
- fixed-tick movement/service timing, capacity enforcement, command ordering, terminal rules, route traces, and all score components;
- nearest-task, earliest-deadline-first, and capacity-aware ordering plus deterministic whole-run results;
- exact-plan replay, optimal task abandonment when its penalty is cheaper, and hard scale rejection;
- canonical replay round trips, identical final hashes, tick corruption, scenario corruption, early and late command corruption;
- save/load reconstruction and altered save hash rejection;
- seeded generation equality/difference properties;
- comparison reports and CLI success/error contracts.

## Build gates

`./gradlew clean check --no-daemon` is the local and CI gate. Java compilation enables all lint warnings and treats warnings as errors. JaCoCo HTML and XML reports are generated for inspection, but this version does not advertise or enforce a coverage percentage.

The desktop module is compiled by the same gate. Rendering is verified separately by the screenshot mode, which starts the actual LWJGL3 application, renders a completed deterministic run, reads the physical back buffer (including Retina scale), writes a PNG, and exits.

## Evidence boundaries

- Test counts should be taken from the current JUnit XML output, not copied forward into documentation.
- No throughput or solver-performance number is claimed without a dedicated benchmark methodology.
- Exact-solver correctness tests use small fixtures appropriate to the solver's stated boundary; they do not establish scalability.
