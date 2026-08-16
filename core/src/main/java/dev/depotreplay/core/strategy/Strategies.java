package dev.depotreplay.core.strategy;

import java.util.List;

public final class Strategies {
    private Strategies() { }

    public static List<DispatchStrategy> baselines() {
        return List.of(
                new NearestTaskStrategy(),
                new EarliestDeadlineFirstStrategy(),
                new CapacityAwareStrategy()
        );
    }

    public static DispatchStrategy named(String id) {
        return baselines().stream()
                .filter(strategy -> strategy.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown strategy '" + id + "'. Expected nearest-task, earliest-deadline-first, or capacity-aware"
                ));
    }
}
