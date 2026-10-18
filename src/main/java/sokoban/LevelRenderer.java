package sokoban;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JPanel;

/** Draws the shared state's current board. Painting must never change game data. */
public final class LevelRenderer extends JPanel {
    // Shrink large boards to fit, but do not enlarge artwork past its native size.
    private static final int MAX_TILE_SIZE = WarehouseSprites.TILE_SIZE;
    private static final int PADDING = 16;
    // All renderer instances reuse the same decoded artwork.
    private static final WarehouseSprites SPRITES = new WarehouseSprites();
    // Edge colors add depth to the tiled artwork; floor shadows are translucent.
    private static final Color WALL_BORDER = new Color(48, 28, 25);
    private static final Color WALL_HIGHLIGHT = new Color(132, 77, 57);
    private static final Color FLOOR_SHADOW = new Color(67, 42, 25, 45);
    private final State state;
    // Cache terrain separately from the moving occupants in the mutable state board.
    private List<String> originalLevel;
    private WarehouseLayout layout;

    /** Attach the shared model and suggest an initial drawing area to the frame. */
    public LevelRenderer(State state) {
        this.state = state;
        setBackground(new Color(143, 143, 135));
        setPreferredSize(new Dimension(1100, 850));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        // Let Swing clear the old frame before drawing the current board.
        super.paintComponent(graphics);
        // Use saved geometry so moving crates cannot change the flooring.
        if (originalLevel != state.getCurrentLevel()) {
            // State returns saved list instances, so identity detects a different layout.
            originalLevel = state.getCurrentLevel();
            layout = new WarehouseLayout(originalLevel);
        }
        int rows = layout.getRowCount();
        int columns = layout.getColumnCount();
        // Fit both dimensions inside the padding; guards avoid zero divisors/tile sizes.
        int tileSize = Math.max(1, Math.min(MAX_TILE_SIZE, Math.min((getWidth() - PADDING * 2) / Math.max(1, columns), (getHeight() - PADDING * 2) / Math.max(1, rows))));
        // Center the resulting board rather than anchoring it to the top-left corner.
        int offsetX = (getWidth() - columns * tileSize) / 2;
        int offsetY = (getHeight() - rows * tileSize) / 2;

        // Work on a copy so our rendering hints and colors do not affect Swing's context.
        Graphics2D canvas = (Graphics2D) graphics.create();
        try {
            // Keep scaled sprites and hand-drawn details aligned to crisp pixel edges.
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
            // Continue cement beyond the map, aligned to the same tile grid.
            for (int y = Math.floorMod(offsetY, tileSize) - tileSize; y < getHeight(); y += tileSize) {
                for (int x = Math.floorMod(offsetX, tileSize) - tileSize; x < getWidth(); x += tileSize) {
                    canvas.drawImage(SPRITES.concrete, x, y, tileSize, tileSize, null);
                }
            }
            // Draw actual row cells over the concrete; ragged padding stays concrete.
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < state.getColumnCount(row); column++) {
                    // Convert board coordinates into this panel's pixel coordinates.
                    int x = offsetX + column * tileSize;
                    int y = offsetY + row * tileSize;
                    if (layout.isWall(row, column)) {
                        drawWall(canvas, row, column, x, y, tileSize);
                    } else {
                        drawFloorAndOccupant(canvas, row, column, x, y, tileSize);
                    }
                }
            }
        } finally {
            // Always release the copied context, even if a draw operation fails.
            canvas.dispose();
        }
    }

    /** Draw a wall tile and outline only the sides facing non-wall neighbors. */
    private void drawWall(Graphics2D canvas, int row, int column, int x, int y, int size) {
        canvas.drawImage(SPRITES.wall, x, y, size, size, null);
        int border = Math.max(1, size / 24);
        // Only exposed edges get outlines; adjoining bricks form continuous walls.
        canvas.setColor(WALL_BORDER);
        if (!layout.isWall(row, column - 1)) {
            canvas.fillRect(x, y, border, size);
        }
        if (!layout.isWall(row, column + 1)) {
            canvas.fillRect(x + size - border, y, border, size);
        }
        if (!layout.isWall(row + 1, column)) {
            canvas.fillRect(x, y + size - border, size, border);
        }
        if (!layout.isWall(row - 1, column)) {
            canvas.fillRect(x, y, size, border);
            if (size >= 16) {
                // Add a one-pixel top highlight only when the tile is large enough.
                canvas.setColor(WALL_HIGHLIGHT);
                canvas.fillRect(x + border, y + border, size - border * 2, 1);
            }
        }
    }

    /** Layer indoor flooring, wall shadows, movable objects, and uncovered goals. */
    private void drawFloorAndOccupant(Graphics2D canvas, int row, int column, int x, int y, int size) {
        // Outdoor spaces retain the concrete background painted earlier.
        if (!layout.isExterior(row, column)) {
            canvas.drawImage(SPRITES.floor, x, y, size, size, null);
            int shadow = Math.max(1, size / 12);
            canvas.setColor(FLOOR_SHADOW);
            // Neighboring walls cast narrow shadows onto the top and left floor edges.
            if (layout.isWall(row - 1, column)) {
                canvas.fillRect(x, y, size, shadow);
            }
            if (layout.isWall(row, column - 1)) {
                canvas.fillRect(x, y, shadow, size);
            }
        }

        // Occupants come from live state, while the flooring comes from saved geometry.
        char tile = state.getTile(row, column);
        if (tile == '$' || tile == '*') {
            canvas.drawImage(tile == '*' ? SPRITES.solvedCrate : SPRITES.crate, x, y, size, size, null);
        } else if (tile == '@' || tile == '+') {
            canvas.drawImage(SPRITES.player, x, y, size, size, null);
        }
        // A crate on a goal (*) uses its solved sprite and hides the goal marker.
        if (tile == '.' || tile == '+') {
            // A player does not solve a goal; keep its dot visible at their feet.
            int centerY = tile == '+' ? y + size * 7 / 8 : y + size / 2;
            drawGoal(canvas, x + size / 2, centerY, size);
        }
    }

    /** Draw a small outlined white marker that stays visible as tiles shrink. */
    private void drawGoal(Graphics2D canvas, int centerX, int centerY, int tileSize) {
        int diameter = Math.max(2, tileSize / 8);
        int x = centerX - diameter / 2;
        int y = centerY - diameter / 2;
        // Two overlapping border rectangles surround the white center by one pixel.
        canvas.setColor(new Color(103, 82, 57));
        canvas.fillRect(x - 1, y, diameter + 2, diameter);
        canvas.fillRect(x, y - 1, diameter, diameter + 2);
        canvas.setColor(Color.WHITE);
        canvas.fillRect(x, y, diameter, diameter);
        // Clip the corners for a tiny round dot on the pixel grid.
        if (diameter >= 4) {
            canvas.setColor(new Color(103, 82, 57));
            canvas.fillRect(x, y, 1, 1);
            canvas.fillRect(x + diameter - 1, y, 1, 1);
            canvas.fillRect(x, y + diameter - 1, 1, 1);
            canvas.fillRect(x + diameter - 1, y + diameter - 1, 1, 1);
        }
    }
}
