# ADR 0005: Regenerating deterministic benchmark reports

- Status: Accepted
- Date: 2026-08-27

## Context

Comparing strategies on one hand-authored scenario can favor a convenient example and does not produce reusable evidence. Wall-clock timing is also a poor default metric for three simple baselines because it depends on the host and says nothing about route quality. A stored aggregate alone could be edited or become incompatible after engine or generator changes.

## Decision

DepotReplay evaluates all built-in baselines over a consecutive range of explicit 64-bit seeds from a versioned scenario generator. The report is canonical JSON and records the simulation contract version, generator ID, scenario hashes, full score breakdowns, final-state hashes, ordered strategy IDs, and integer aggregates.

Verification does not trust stored aggregates. It checks the artifact versions, regenerates every scenario, reruns every strategy through `SimulationEngine`, and compares the complete reconstructed report. Seed ranges contain 1 to 1,000 scenarios and may not overflow signed 64-bit integers. Tied minimum scores count as best for every tied strategy.

## Consequences

- Reviewers can reproduce a fixed evaluation artifact without external data or services.
- CI can publish behavioral evidence without making host-dependent speed claims.
- Engine or generator semantics must receive a new version before intentionally incompatible artifacts are produced.
- The suite compares only the included synthetic generator and transparent baselines; it does not establish real-world performance, global optimality, or scalability.
