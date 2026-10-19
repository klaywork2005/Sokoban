package sokoban;

import java.util.ArrayDeque;
import java.util.List;

/**
 * The fixed geometry of a level: where the walls are, and which empty squares are
 * outdoors rather than inside the warehouse. It is built from the saved level rows
 * before anything moves, so pushing boxes can never change the flooring.
 *
 * <p>Level rows can have different lengths, so everything is stored in a rectangle
 * as wide as the longest row. Missing squares count as blank padding.
 */
final class WarehouseLayout {
    /** One square of the grid, used while flood-filling the outdoor area. */
    private record Cell(int row, int column) { }

    private final int rowCount;
    private final int columnCount;
    private final boolean[][] walls;
    // True for blank squares connected to the edge of the rectangle: the outdoor area.
    private final boolean[][] exterior;

    /** Work out the walls and outdoor area of a level from its saved rows. */
    WarehouseLayout(List<String> rows) {
        rowCount = rows.size();
        columnCount = BoardRules.widestRowLength(rows);
        walls = new boolean[rowCount][columnCount];
        exterior = new boolean[rowCount][columnCount];

        // Only literal spaces can be outdoors: goals, boxes and the player block the flood.
        boolean[][] blank = new boolean[rowCount][columnCount];
        findWallsAndBlanks(rows, blank);
        markExterior(blank);
    }

    /** Height of the level, including any empty rows. */
    int getRowCount() {
        return rowCount;
    }

    /** Width of the bounding rectangle, including padding after shorter rows. */
    int getColumnCount() {
        return columnCount;
    }

    /** True for outdoor squares. Anything beyond the rectangle is also outdoors. */
    boolean isExterior(int row, int column) {
        return !contains(row, column) || exterior[row][column];
    }

    /** True only where the saved level has a wall; squares beyond the rectangle are not walls. */
    boolean isWall(int row, int column) {
        return contains(row, column) && walls[row][column];
    }

    /** Check that a position is inside the rectangle before it is used as an array index. */
    private boolean contains(int row, int column) {
        return row >= 0 && row < rowCount && column >= 0 && column < columnCount;
    }

    /** Fill in the wall and blank masks. Squares missing from short rows count as blank. */
    private void findWallsAndBlanks(List<String> rows, boolean[][] blank) {
        for (int row = 0; row < rowCount; row++) {
            String text = rows.get(row);
            for (int column = 0; column < columnCount; column++) {
                char tile = column < text.length() ? text.charAt(column) : Tiles.FLOOR;
                walls[row][column] = tile == Tiles.WALL;
                blank[row][column] = tile == Tiles.FLOOR;
            }
        }
    }

    /**
     * Mark every blank square connected to the rectangle's edge as outdoors.
     * The fill starts from outside rather than from the player, so an empty
     * room sealed behind walls is still treated as indoor floor.
     */
    private void markExterior(boolean[][] blank) {
        ArrayDeque<Cell> pending = new ArrayDeque<>();

        // Seed the fill with every blank square on the edge of the rectangle.
        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                boolean onEdge = row == 0 || row == rowCount - 1 || column == 0 || column == columnCount - 1;
                if (blank[row][column] && onEdge) {
                    exterior[row][column] = true; // Mark when queued so each square is queued once.
                    pending.add(new Cell(row, column));
                }
            }
        }

        // Spread through the four neighbors of each square, never diagonally.
        while (!pending.isEmpty()) {
            Cell cell = pending.removeFirst();
            spreadTo(cell.row() - 1, cell.column(), blank, pending);
            spreadTo(cell.row() + 1, cell.column(), blank, pending);
            spreadTo(cell.row(), cell.column() - 1, blank, pending);
            spreadTo(cell.row(), cell.column() + 1, blank, pending);
        }
    }

    /** Add a neighbor to the flood fill if it is a blank square not reached yet. */
    private void spreadTo(int row, int column, boolean[][] blank, ArrayDeque<Cell> pending) {
        // contains() is checked first so an out-of-range index is never used.
        if (contains(row, column) && blank[row][column] && !exterior[row][column]) {
            exterior[row][column] = true;
            pending.addLast(new Cell(row, column));
        }
    }
}
