package dev.depotreplay.game;

/** Platform boundary for optional rendered-frame capture. */
public interface FrameCapture {
    boolean requested();

    void afterFrame();
}
