package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/** Paint into images on Swing's event thread and verify the resulting pixels. */
class LevelRendererTest {
    // Match native sprite size and renderer padding for predictable tile extraction.
    private static final int TILE = 48;
    private static final int MARGIN = 16;

    @Test
    void repaintMovesThePlayerSpriteAndRestoresTheWoodFloor() throws Exception {
        // invokeAndWait keeps Swing access on its event thread and waits for assertions.
        SwingUtilities.invokeAndWait(() -> {
            State state = new State(List.of(List.of(
                    "#######", "#     #", "# @   #", "#     #", "#######")));
            GameLogic game = new GameLogic(state);
            LevelRenderer renderer = renderer(state, 7, 5, TILE);
            // Capture the same renderer before and after a move, with no window needed.
            BufferedImage before = paint(renderer);

            game.moveCharacter(0, 1);
            BufferedImage after = paint(renderer);

            // The player and bare-floor images should trade places after the move.
            assertTileEquals(tile(before, 2, 2, TILE), tile(after, 2, 3, TILE));
            assertTileEquals(tile(before, 2, 3, TILE), tile(after, 2, 2, TILE));
            assertSpriteVisible(new WarehouseSprites().player, tile(after, 2, 3, TILE));
            assertFalse(java.util.Arrays.equals(pixels(tile(before, 2, 2, TILE)),
                    pixels(tile(after, 2, 2, TILE))), "The player must leave their old tile.");
        });
    }

    @Test
    void repaintReadsNewLevelGeometryAndOccupantsFromTheSharedState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // Keep equal bounding dimensions while changing walls and outdoor spaces.
            State state = new State(List.of(
                    List.of("  ###", "  #@#", "  ###"),
                    List.of("#####", "#@  #", "#####")));
            LevelRenderer renderer = renderer(state, 5, 3, TILE);
            WarehouseSprites sprites = new WarehouseSprites();
            BufferedImage before = paint(renderer);
            assertTileEquals(sprites.concrete, tile(before, 1, 1, TILE));

            // Reuse the renderer to verify its cached layout follows the selected level.
            new GameLogic(state).selectLevel(1);
            BufferedImage after = paint(renderer);

            assertSpriteVisible(sprites.player, tile(after, 1, 1, TILE));
            assertEquals(sprites.wall.getRGB(TILE / 2, TILE / 2),
                    tile(after, 0, 0, TILE).getRGB(TILE / 2, TILE / 2));
            assertFalse(java.util.Arrays.equals(pixels(before), pixels(after)));
        });
    }

    @Test
    void smallWhiteGoalDotDisappearsUnderRedwoodCrateAndReturnsAfterThePush() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // A straight corridor lets two pushes cover and then uncover one goal.
            State state = new State(List.of(List.of("#######", "#@$.  #", "#######")));
            GameLogic game = new GameLogic(state);
            LevelRenderer renderer = renderer(state, 7, 3, TILE);
            WarehouseSprites sprites = new WarehouseSprites();
            BufferedImage before = paint(renderer);
            BufferedImage unsolvedGoal = tile(before, 1, 3, TILE);
            BufferedImage normalCrate = tile(before, 1, 2, TILE);
            assertSmallWhiteDot(unsolvedGoal, 8);
            assertSpriteVisible(sprites.crate, normalCrate);

            // A crate on a goal changes artwork and fully covers the white marker.
            game.moveCharacter(0, 1);
            BufferedImage solvedCrate = tile(paint(renderer), 1, 3, TILE);
            assertEquals(0, whitePixels(solvedCrate), "A covered goal must have no white dot.");
            assertSpriteVisible(sprites.solvedCrate, solvedCrate);
            assertFalse(java.util.Arrays.equals(pixels(normalCrate), pixels(solvedCrate)),
                    "The crate should change to its solved wood color.");

            // After another push the player is on the goal, so its foot-level dot returns.
            game.moveCharacter(0, 1);
            BufferedImage pushedOffGoal = paint(renderer);
            assertTileEquals(normalCrate, tile(pushedOffGoal, 1, 4, TILE));
            assertSmallWhiteDot(tile(pushedOffGoal, 1, 3, TILE), 8);

            // Walking away must restore the exact original uncovered-goal appearance.
            game.moveCharacter(0, -1);
            assertTileEquals(unsolvedGoal, tile(paint(renderer), 1, 3, TILE));
        });
    }

    @Test
    void leadingSpacesAndRaggedPaddingUseConcreteWhileEnclosedFloorUsesWood() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // This irregular outline includes both literal outside spaces and absent cells.
            State state = new State(List.of(List.of(
                    "  #####", "  #@ .#", "###   #", "# $ ###", "#####")));
            LevelRenderer renderer = renderer(state, 7, 5, TILE);
            WarehouseSprites sprites = new WarehouseSprites();
            BufferedImage image = paint(renderer);

            // Outdoor tiles match concrete exactly; an interior tile matches bare wood.
            assertTileEquals(sprites.concrete, tile(image, 1, 0, TILE));
            assertTileEquals(sprites.concrete, tile(image, 4, 5, TILE));
            assertTileEquals(sprites.concrete, tile(image, 4, 6, TILE));
            assertTileEquals(sprites.floor, tile(image, 2, 4, TILE));
            // The margin continues the same concrete texture outside the map.
            assertEquals(sprites.concrete.getRGB(TILE - MARGIN, TILE - MARGIN),
                    image.getRGB(0, 0));
        });
    }

    @Test
    void resizingKeepsGoalsVisibleAndRepeatedPaintingDoesNotChangeTheBoard() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            State state = new State(List.of(List.of(
                    "#####", "# @ #", "# . #", "# $ #", "#####")));
            // Preserve both representations so painting can be checked for side effects.
            List<String> original = List.copyOf(state.getCurrentLevel());
            List<String> board = snapshot(state);
            LevelRenderer renderer = renderer(state, 5, 5, 23);

            // Verify the goal at a reduced tile size and deterministic repeated painting.
            BufferedImage first = paint(renderer);
            assertSmallWhiteDot(tile(first, 2, 2, 23), 4);
            assertArrayEquals(pixels(first), pixels(paint(renderer)));

            // Restore native size and ensure neither board data nor level selection changed.
            renderer.setSize(5 * TILE + MARGIN * 2, 5 * TILE + MARGIN * 2);
            assertSmallWhiteDot(tile(paint(renderer), 2, 2, TILE), 8);
            assertEquals(board, snapshot(state));
            assertEquals(original, state.getCurrentLevel());
            assertEquals(0, state.getCurrentLevelIndex());
        });
    }

    /** Size a panel so a board with these dimensions fits at the requested tile size. */
    private LevelRenderer renderer(State state, int columns, int rows, int tileSize) {
        LevelRenderer renderer = new LevelRenderer(state);
        renderer.setSize(columns * tileSize + MARGIN * 2, rows * tileSize + MARGIN * 2);
        return renderer;
    }

    /** Render synchronously into an offscreen image for deterministic pixel checks. */
    private BufferedImage paint(LevelRenderer renderer) {
        BufferedImage image = new BufferedImage(renderer.getWidth(), renderer.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            renderer.paint(graphics);
        } finally {
            // The image remains available after its temporary graphics context is released.
            graphics.dispose();
        }
        return image;
    }

    /** Extract one board cell, accounting for the surrounding panel margin. */
    private BufferedImage tile(BufferedImage image, int row, int column, int tileSize) {
        return image.getSubimage(MARGIN + column * tileSize, MARGIN + row * tileSize,
                tileSize, tileSize);
    }

    /** Flatten image colors into row order for exact comparisons. */
    private int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    /** Compare the complete tile image rather than a few representative pixels. */
    private void assertTileEquals(BufferedImage expected, BufferedImage actual) {
        assertArrayEquals(pixels(expected), pixels(actual));
    }

    /** Check the sprite's opaque interior pixels against its composition over wood. */
    private void assertSpriteVisible(BufferedImage sprite, BufferedImage tile) {
        BufferedImage expected = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = expected.createGraphics();
        try {
            // Build the expected appearance with the same transparent-image layering.
            graphics.drawImage(new WarehouseSprites().floor, 0, 0, null);
            graphics.drawImage(sprite, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        int compared = 0;
        // Ignore the edge where adjacent walls can cast a floor shadow.
        for (int y = 8; y < sprite.getHeight(); y++) {
            for (int x = 8; x < sprite.getWidth(); x++) {
                int color = sprite.getRGB(x, y);
                if ((color >>> 24) >= 200) {
                    // The high byte is alpha; ignore mostly transparent edge pixels.
                    assertEquals(expected.getRGB(x, y), tile.getRGB(x, y),
                            "Visible sprite pixel at " + x + "," + y);
                    compared++;
                }
            }
        }
        // Prevent an empty or unexpectedly transparent asset from passing vacuously.
        assertTrue(compared > 100, "The rendered sprite must have a substantial visible silhouette.");
    }

    /** Count pure white pixels used by the uncovered-goal marker. */
    private int whitePixels(BufferedImage image) {
        int count = 0;
        for (int pixel : pixels(image)) {
            if (pixel == Color.WHITE.getRGB()) {
                count++;
            }
        }
        return count;
    }

    /** Ensure a goal marker is visible but stays within a small bounding rectangle. */
    private void assertSmallWhiteDot(BufferedImage image, int maximumSize) {
        // Initialize empty bounds and expand them for every white marker pixel found.
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == Color.WHITE.getRGB()) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        // Presence alone is insufficient: an oversized white patch must also fail.
        assertTrue(whitePixels(image) >= 4, "An unsolved goal must retain a visible white dot.");
        assertTrue(maxX - minX + 1 <= maximumSize, "The goal dot must remain narrow.");
        assertTrue(maxY - minY + 1 <= maximumSize, "The goal dot must remain short.");
    }

    /** Copy the live board into strings without exposing or changing its internal array. */
    private List<String> snapshot(State state) {
        List<String> rows = new ArrayList<>();
        for (int row = 0; row < state.getRowCount(); row++) {
            // Respect ragged row lengths instead of padding the saved snapshot.
            StringBuilder text = new StringBuilder();
            for (int column = 0; column < state.getColumnCount(row); column++) {
                text.append(state.getTile(row, column));
            }
            rows.add(text.toString());
        }
        return rows;
    }
}
