package dev.depotreplay.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import dev.depotreplay.game.FrameCapture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class PngFrameCapture implements FrameCapture {
    private final Path screenshotPath;
    private int renderedFrames;
    private boolean written;

    PngFrameCapture(Path screenshotPath) {
        this.screenshotPath = screenshotPath;
    }

    @Override
    public boolean requested() {
        return true;
    }

    @Override
    public void afterFrame() {
        if (written || renderedFrames++ < 3) {
            return;
        }
        try {
            Path absolute = screenshotPath.toAbsolutePath().normalize();
            Path parent = absolute.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            int width = Gdx.graphics.getBackBufferWidth();
            int height = Gdx.graphics.getBackBufferHeight();
            byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, width, height, true);
            Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
            BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
            try {
                PixmapIO.writePNG(new FileHandle(absolute.toFile()), pixmap);
            } finally {
                pixmap.dispose();
            }
            written = true;
            Gdx.app.log("DepotReplay", "Screenshot written to " + absolute);
        } catch (RuntimeException | IOException error) {
            Gdx.app.error("DepotReplay", "Could not write screenshot", error);
        } finally {
            Gdx.app.exit();
        }
    }
}
