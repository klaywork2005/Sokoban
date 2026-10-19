package sokoban;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * The game's artwork: PNG files loaded once from the classpath and scaled to one
 * tile size. The fields are package-private so {@link LevelRenderer} and the tests
 * can read the images directly.
 */
final class WarehouseSprites {
    /** Every sprite is normalized to this many pixels wide and tall. */
    static final int TILE_SIZE = 48;

    // Classpath folder that holds the PNG files.
    private static final String SPRITE_FOLDER = "/sokoban/tiles/";

    // Each field loads its image when a WarehouseSprites object is created.
    final BufferedImage wall = load("wall.png");
    final BufferedImage floor = load("floor.png");
    final BufferedImage concrete = load("concrete.png");
    final BufferedImage crate = load("crate.png");
    // A different crate image shows that the goal underneath is covered.
    final BufferedImage solvedCrate = load("crate-goal.png");
    final BufferedImage player = load("player.png");

    /**
     * Load one PNG from the sprite folder and scale it to a tile.
     *
     * @throws IllegalStateException if the file is missing or is not a valid image.
     *                               The game cannot draw without its sprites.
     */
    private static BufferedImage load(String name) {
        String path = SPRITE_FOLDER + name;
        try (InputStream stream = WarehouseSprites.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing resource: " + path);
            }
            BufferedImage source = ImageIO.read(stream);
            if (source == null) {
                throw new IOException("Invalid image: " + path); // ImageIO could not decode it.
            }
            return scaleToTile(source);
        } catch (IOException exception) {
            // Keep the file path and the original cause so the problem is easy to find.
            throw new IllegalStateException("Could not load warehouse sprite " + path, exception);
        }
    }

    /** Redraw an image at exactly {@link #TILE_SIZE} pixels square. */
    private static BufferedImage scaleToTile(BufferedImage source) {
        // ARGB keeps the transparent pixels around the player and crate shapes.
        BufferedImage sprite = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sprite.createGraphics();
        try {
            // Nearest-neighbor scaling keeps hard pixel edges instead of blurring them.
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(source, 0, 0, TILE_SIZE, TILE_SIZE, null);
        } finally {
            graphics.dispose(); // Always release the drawing context, even if drawing fails.
        }
        return sprite;
    }
}
