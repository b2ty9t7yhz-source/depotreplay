package dev.depotreplay.core.path;

import dev.depotreplay.core.model.Position;

import java.io.Serial;

public final class PathNotFoundException extends IllegalStateException {
    @Serial
    private static final long serialVersionUID = 1L;

    public PathNotFoundException(Position start, Position goal) {
        super("No traversable path from " + start + " to " + goal);
    }
}
