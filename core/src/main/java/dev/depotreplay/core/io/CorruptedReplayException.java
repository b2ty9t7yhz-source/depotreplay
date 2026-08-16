package dev.depotreplay.core.io;

import java.io.Serial;

public final class CorruptedReplayException extends DepotReplayException {
    @Serial
    private static final long serialVersionUID = 1L;

    public CorruptedReplayException(String message) {
        super(message);
    }

    public CorruptedReplayException(String message, Throwable cause) {
        super(message, cause);
    }
}
