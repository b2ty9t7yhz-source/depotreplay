package dev.depotreplay.core.io;

import java.io.Serial;

public class DepotReplayException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public DepotReplayException(String message) {
        super(message);
    }

    public DepotReplayException(String message, Throwable cause) {
        super(message, cause);
    }
}
