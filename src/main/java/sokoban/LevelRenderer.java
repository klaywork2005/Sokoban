package sokoban;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JPanel;

/**
 * Draws the shared {@link State}'s current board. Painting only reads game
 * data;
 * it must never change it.
 *
 * <p>
 * Two sources are combined: the saved level supplies the fixed terrain
 * (walls, indoor floor, outdoor concrete), and the live board supplies the
 * things
 * that move (boxes and the player).
 */
public final class LevelRenderer extends JPanel {
  // Swing components are Serializable, so the compiler expects a version number.
  // This panel is never actually saved to disk.
  private static final long serialVersionUID = 1L;

  // Panel size and colors.
  private static final int PREFERRED_WIDTH = 1100;
  private static final int PREFERRED_HEIGHT = 850;
  private static final Color BACKGROUND = new Color(143, 143, 135);

  // Boards shrink to fit the window but are never enlarged past the artwork's
  // native size.
  private static final int MAX_TILE_SIZE = WarehouseSprites.TILE_SIZE;
  // Empty border kept around the board, in pixels.
  private static final int PADDING = 16;

  // Wall edges and floor shadows add depth to the tiled artwork.
  private static final Color WALL_BORDER = new Color(48, 28, 25);
  private static final Color WALL_HIGHLIGHT = new Color(132, 77, 57);
  private static final Color FLOOR_SHADOW = new Color(67, 42, 25, 45); // Translucent.
  // The dark outline drawn around a goal's white dot.
  private static final Color GOAL_OUTLINE = new Color(103, 82, 57);

  // All renderers share one set of decoded images.
  private static final WarehouseSprites SPRITES = new WarehouseSprites();

  private final State state;
  // The terrain is rebuilt only when a different level is selected.
  private List<String> terrainSource;
  private WarehouseLayout layout;

  /** Attach the shared game state and suggest an initial size to the window. */
  public LevelRenderer(State state) {
    this.state = state;
    setBackground(BACKGROUND);
    setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    super.paintComponent(graphics); // Let Swing clear the previous frame first.
    updateLayoutIfLevelChanged();

    int rows = layout.getRowCount();
    int columns = layout.getColumnCount();
    int tileSize = calculateTileSize(rows, columns);
    // Center the board instead of anchoring it to the top-left corner.
    int offsetX = (getWidth() - columns * tileSize) / 2;
    int offsetY = (getHeight() - rows * tileSize) / 2;

    // Draw on a copy so our rendering settings do not leak into Swing's own
    // drawing.
    Graphics2D canvas = (Graphics2D) graphics.create();
    try {
      // Nearest-neighbor scaling and no smoothing keep the pixel art crisp.
      canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_OFF);
      drawConcreteBackground(canvas, offsetX, offsetY, tileSize);
      drawBoard(canvas, rows, offsetX, offsetY, tileSize);
    } finally {
      canvas.dispose(); // Always release the copy, even if drawing fails.
    }
  }

  /** Rebuild the cached terrain when the player has switched to another level. */
  private void updateLayoutIfLevelChanged() {
    // State returns the same list object until the level changes, so a plain
    // reference comparison (!=) is enough to detect a different level.
    if (terrainSource != state.getCurrentLevel()) {
      terrainSource = state.getCurrentLevel();
      layout = new WarehouseLayout(terrainSource);
    }
  }

  /** The largest tile size that fits the whole board inside the padding. */
  private int calculateTileSize(int rows, int columns) {
    int availableWidth = getWidth() - PADDING * 2;
    int availableHeight = getHeight() - PADDING * 2;
    // Math.max(1, ...) prevents dividing by zero for an empty level.
    int widthPerTile = availableWidth / Math.max(1, columns);
    int heightPerTile = availableHeight / Math.max(1, rows);
    int fittingSize = Math.min(widthPerTile, heightPerTile);
    // Never smaller than one pixel and never larger than the artwork.
    return Math.max(1, Math.min(MAX_TILE_SIZE, fittingSize));
  }

  /**
   * Tile concrete over the whole panel so the area outside the map is not empty.
   */
  private void drawConcreteBackground(Graphics2D canvas, int offsetX, int offsetY, int tileSize) {
    // Start one tile before the edge, aligned with the board's grid, so the pattern
    // lines up.
    int startY = Math.floorMod(offsetY, tileSize) - tileSize;
    int startX = Math.floorMod(offsetX, tileSize) - tileSize;
    for (int y = startY; y < getHeight(); y += tileSize) {
      for (int x = startX; x < getWidth(); x += tileSize) {
        canvas.drawImage(SPRITES.concrete, x, y, tileSize, tileSize, null);
      }
    }
  }

  /** Draw every square of the board over the concrete. */
  private void drawBoard(Graphics2D canvas, int rows, int offsetX, int offsetY, int tileSize) {
    for (int row = 0; row < rows; row++) {
      // Only draw squares that exist in the row; the padding after short rows stays
      // concrete.
      for (int column = 0; column < state.getColumnCount(row); column++) {
        // Convert board coordinates into pixel coordinates on this panel.
        int x = offsetX + column * tileSize;
        int y = offsetY + row * tileSize;
        if (layout.isWall(row, column)) {
          drawWall(canvas, row, column, x, y, tileSize);
        } else {
          drawFloorAndOccupant(canvas, row, column, x, y, tileSize);
        }
      }
    }
  }

  /**
   * Draw a wall tile and outline only the sides that face a non-wall neighbor.
   */
  private void drawWall(Graphics2D canvas, int row, int column, int x, int y, int size) {
    canvas.drawImage(SPRITES.wall, x, y, size, size, null);
    int border = Math.max(1, size / 24);

    // Only exposed edges get an outline, so neighboring walls blend into one block.
    canvas.setColor(WALL_BORDER);
    if (!layout.isWall(row, column - 1)) {
      canvas.fillRect(x, y, border, size); // Left edge.
    }
    if (!layout.isWall(row, column + 1)) {
      canvas.fillRect(x + size - border, y, border, size); // Right edge.
    }
    if (!layout.isWall(row + 1, column)) {
      canvas.fillRect(x, y + size - border, size, border); // Bottom edge.
    }
    if (!layout.isWall(row - 1, column)) {
      canvas.fillRect(x, y, size, border); // Top edge.
      if (size >= 16) {
        // A one-pixel highlight under the top edge, only when the tile is big enough.
        canvas.setColor(WALL_HIGHLIGHT);
        canvas.fillRect(x + border, y + border, size - border * 2, 1);
      }
    }
  }

  /**
   * Layer indoor flooring, wall shadows, boxes or the player, and any uncovered
   * goal.
   */
  private void drawFloorAndOccupant(
      Graphics2D canvas, int row, int column, int x, int y, int size) {
    // Outdoor squares keep the concrete that was painted earlier.
    if (!layout.isExterior(row, column)) {
      drawIndoorFloor(canvas, row, column, x, y, size);
    }

    // What stands on the square comes from the live board; the flooring comes from
    // the
    // saved level.
    char tile = state.getTile(row, column);
    if (Tiles.isBox(tile)) {
      // A box on a goal (*) uses its own sprite, which also hides the goal marker.
      canvas.drawImage(tile == Tiles.BOX_ON_GOAL ? SPRITES.solvedCrate : SPRITES.crate,
          x, y, size, size, null);
    } else if (Tiles.isPlayer(tile)) {
      canvas.drawImage(SPRITES.player, x, y, size, size, null);
    }

    // Show the goal marker unless a box covers it. A player does not cover the
    // marker.
    if (tile == Tiles.GOAL || tile == Tiles.PLAYER_ON_GOAL) {
      // With the player present, draw the dot near their feet so both stay visible.
      int dotCenterY = tile == Tiles.PLAYER_ON_GOAL ? y + size * 7 / 8 : y + size / 2;
      drawGoal(canvas, x + size / 2, dotCenterY, size);
    }
  }

  /**
   * Draw the wooden floor, with a narrow shadow where walls sit above or to the
   * left.
   */
  private void drawIndoorFloor(Graphics2D canvas, int row, int column, int x, int y, int size) {
    canvas.drawImage(SPRITES.floor, x, y, size, size, null);
    int shadow = Math.max(1, size / 12);
    canvas.setColor(FLOOR_SHADOW);
    if (layout.isWall(row - 1, column)) {
      canvas.fillRect(x, y, size, shadow); // Shadow along the top edge.
    }
    if (layout.isWall(row, column - 1)) {
      canvas.fillRect(x, y, shadow, size); // Shadow along the left edge.
    }
  }

  /**
   * Draw a small white dot with a dark outline that stays visible as tiles
   * shrink.
   */
  private void drawGoal(Graphics2D canvas, int centerX, int centerY, int tileSize) {
    int diameter = Math.max(2, tileSize / 8);
    int x = centerX - diameter / 2;
    int y = centerY - diameter / 2;

    // Two overlapping rectangles form a plus-shaped outline one pixel larger than
    // the dot.
    canvas.setColor(GOAL_OUTLINE);
    canvas.fillRect(x - 1, y, diameter + 2, diameter);
    canvas.fillRect(x, y - 1, diameter, diameter + 2);
    canvas.setColor(Color.WHITE);
    canvas.fillRect(x, y, diameter, diameter);

    // Cut the four corners with outline-colored pixels so the dot looks round.
    if (diameter >= 4) {
      canvas.setColor(GOAL_OUTLINE);
      canvas.fillRect(x, y, 1, 1);
      canvas.fillRect(x + diameter - 1, y, 1, 1);
      canvas.fillRect(x, y + diameter - 1, 1, 1);
      canvas.fillRect(x + diameter - 1, y + diameter - 1, 1, 1);
    }
  }
}
