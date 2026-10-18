package sokoban;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Classpath assets decoded once and sampled onto the game's pixel grid. */
final class WarehouseSprites {
    // Normalize all source artwork to one logical tile resolution before rendering.
    static final int TILE_SIZE = 48;
    // Instance field initializers load each asset when the sprite collection is created.
    final BufferedImage wall = load("wall.png");
    final BufferedImage floor = load("floor.png");
    final BufferedImage concrete = load("concrete.png");
    final BufferedImage crate = load("crate.png");
    // A different crate image communicates that the underlying goal is covered.
    final BufferedImage solvedCrate = load("crate-goal.png");
    final BufferedImage player = load("player.png");

    /** Decode a bundled PNG and scale it into a tile with an alpha channel. */
    private BufferedImage load(String name) {
        String path = "/sokoban/tiles/" + name;
        try (InputStream stream = WarehouseSprites.class.getResourceAsStream(path)) {
            // A missing resource returns null; ImageIO can also reject undecodable data.
            if (stream == null) {
                throw new IOException("Missing resource: " + path);
            }
            BufferedImage source = ImageIO.read(stream);
            if (source == null) {
                throw new IOException("Invalid image: " + path);
            }
            // ARGB preserves transparent pixels around the player and crate silhouettes.
            BufferedImage sprite = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = sprite.createGraphics();
            try {
                // Nearest-neighbor sampling retains hard pixel edges instead of blurring.
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                graphics.drawImage(source, 0, 0, TILE_SIZE, TILE_SIZE, null);
            } finally {
                // Release the temporary drawing context, including if scaling fails.
                graphics.dispose();
            }
            return sprite;
        } catch (IOException exception) {
            // Sprite failures are fatal to rendering; keep the resource path and root cause.
            throw new IllegalStateException("Could not load warehouse sprite " + path, exception);
        }
    }
}
