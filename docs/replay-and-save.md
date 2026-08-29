# Replay and save format

## Canonical JSON

Canonical values are encoded as UTF-8 JSON with alphabetically ordered object properties, ordered map entries, stable application-defined list ordering, and no indentation. SHA-256 is computed over those exact bytes and represented as 64 lowercase hexadecimal characters.

Canonicalization is an application-level reproducibility contract, not a claim of conformance to an external JSON canonicalization standard.

## Replay

A schema version 1 replay contains:

| Field | Meaning |
| --- | --- |
| `engineVersion` | Compatibility gate for replay semantics |
| `scenario` | Complete validated scenario |
| `scenarioHash` | Canonical SHA-256 of `scenario` |
| `commands` | Stable tick/vehicle/task/action command sequence |
| `tickHashes` | Tick 0 state hash followed by one hash per advanced tick |
| `finalStateHash` | Expected canonical final snapshot hash |

Verification reconstructs an engine, schedules the commands, hashes tick 0, advances until terminal, and compares every expected tick and hash. It also rejects missing/trailing tick hashes, invalid commands, unsupported versions, scenario changes, and final-hash mismatches.

The replay embeds its scenario to avoid resolving mutable external scenario files.

### Browser boundary

The TeaVM build uses a strict reflection-free decoder for this same schema. It rejects missing, unknown, null, wrongly typed, out-of-range, or unsupported enum fields before semantic verification. The browser then calls the same `ReplayService.verify` path used by the CLI and desktop modules. Only after all hashes pass does it construct a fresh engine with the verified commands queued for step-by-step review.

Browser `S` stores an encoded replay in origin-local storage, `L` prefers a replay selected through the page and otherwise uses the stored replay, and `E` downloads the verified JSON. These operations do not upload data. Imports larger than 5 MB are rejected before parsing. Browser load begins at tick 0; it is intentionally not an in-progress checkpoint restore.

## Save

A save file contains a validated scenario, its hash, `savedAtTick`, the applied command log, and the expected state hash. Loading replays the commands only to `savedAtTick`. The service returns a resumable engine only when the reconstructed canonical snapshot hash matches.

Save and replay files are written via a temporary file followed by an atomic replacement where the file system supports it. A non-atomic replacement is used as a documented portability fallback.

## Corruption model

The design detects accidental or intentional changes to hashed scenario or state content. It does not provide authenticity: SHA-256 is unkeyed, so an attacker who can rewrite an artifact can also recompute every hash. Signed artifacts are outside the current scope.
