package dev.depotreplay.core.compare;

import java.util.List;

public record ComparisonReport(String scenarioName, List<RunComparison> runs) {
    public ComparisonReport {
        runs = List.copyOf(runs);
    }
}
