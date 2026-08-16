package dev.depotreplay.core.validation;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public final class ValidationException extends IllegalArgumentException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ArrayList<String> violations;

    public ValidationException(List<String> violations) {
        super("Invalid scenario: " + String.join("; ", violations));
        this.violations = new ArrayList<>(violations);
    }

    public List<String> violations() {
        return List.copyOf(violations);
    }
}
