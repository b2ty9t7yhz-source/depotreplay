package dev.depotreplay.core.exact;

import java.io.Serial;

public final class ExactSolverLimitException extends IllegalArgumentException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ExactSolverLimitException(String message) {
        super(message);
    }
}
