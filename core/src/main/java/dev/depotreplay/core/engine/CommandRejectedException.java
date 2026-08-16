package dev.depotreplay.core.engine;

import java.io.Serial;

public final class CommandRejectedException extends IllegalArgumentException {
    @Serial
    private static final long serialVersionUID = 1L;

    public CommandRejectedException(String message) {
        super(message);
    }
}
