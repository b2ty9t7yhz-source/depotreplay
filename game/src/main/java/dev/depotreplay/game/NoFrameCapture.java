package dev.depotreplay.game;

public final class NoFrameCapture implements FrameCapture {
    @Override
    public boolean requested() {
        return false;
    }

    @Override
    public void afterFrame() {
        // Intentionally empty.
    }
}
