# Scenario format

Scenario files are JSON objects with `schemaVersion: 1`.

```json
{
  "schemaVersion": 1,
  "name": "Small example",
  "seed": 12345,
  "grid": {
    "width": 7,
    "height": 5,
    "obstacles": [{"x": 3, "y": 2}]
  },
  "vehicles": [
    {"id": "V1", "start": {"x": 0, "y": 0}, "capacity": 3},
    {"id": "V2", "start": {"x": 6, "y": 4}, "capacity": 3}
  ],
  "tasks": [
    {
      "id": "T1",
      "pickup": {"x": 1, "y": 0},
      "delivery": {"x": 5, "y": 1},
      "demand": 2,
      "deadline": 12
    }
  ],
  "scoreWeights": {"distance": 1, "lateness": 5, "unserved": 100},
  "maxTicks": 40,
  "pathAlgorithm": "ASTAR"
}
```

## Validation contract

| Field | Rule |
| --- | --- |
| `schemaVersion` | Must equal `1` |
| `name` | 1 to 80 non-blank characters |
| `seed` | Any signed 64-bit integer |
| grid width/height | Each from 2 through 50 |
| obstacles | Unique, non-null, and in bounds |
| vehicles | Exactly two with unique IDs and capacity 1 through 100 |
| tasks | 1 through 100 with unique IDs |
| IDs | 1 through 32 ASCII letters, digits, `_`, or `-` |
| task endpoints | Different, in bounds, and not blocked |
| demand | Positive and no larger than at least one vehicle capacity |
| deadline | From 1 through `maxTicks` |
| score weights | Distance/lateness non-negative; unserved positive |
| `maxTicks` | 1 through 10,000 |
| path algorithm | `DIJKSTRA` or `ASTAR` |
| connectivity | Every vehicle start, pickup, and delivery is mutually reachable |

Unknown JSON fields are rejected by the default Jackson mapping behavior. Numbers outside their Java integer/long ranges are rejected during parsing.

## Seed meaning

The seed is part of canonical scenario and state identity. Hand-authored scenarios retain it as provenance. `generate` uses `SplittableRandom` driven only by that seed, so identical seeds produce identical canonical scenarios.
