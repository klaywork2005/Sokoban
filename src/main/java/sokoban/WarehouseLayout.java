package sokoban;

import java.util.ArrayDeque;
import java.util.List;

/** Fixed wall and outdoor geometry from the original, potentially ragged XSB rows. */
final class WarehouseLayout {
    // Rectangular masks simplify neighbor checks even when source rows vary in length.
    private final int rowCount;
    private final int columnCount;
    private final boolean[][] walls;
    // True only for blank cells reachable from the rectangle's outer boundary.
    private final boolean[][] exterior;

    /** Classify static terrain once, using the saved layout before anything moves. */
    WarehouseLayout(List<String> rows) {
        rowCount = rows.size();
        // The longest row defines the bounding rectangle; an empty level has width zero.
        columnCount = rows.stream().mapToInt(String::length).max().orElse(0);
        walls = new boolean[rowCount][columnCount];
        exterior = new boolean[rowCount][columnCount];
        // Only literal spaces can carry the outdoor flood; goals and occupants block it.
        boolean[][] empty = new boolean[rowCount][columnCount];
        // Queue positions as row * width + column instead of allocating coordinate pairs.
        ArrayDeque<Integer> pending = new ArrayDeque<>();

        // Build the masks and seed the search with empty cells touching the boundary.
        for (int row = 0; row < rowCount; row++) {
            String text = rows.get(row);
            for (int column = 0; column < columnCount; column++) {
                // Missing trailing characters count as blank padding for visual layout.
                char tile = column < text.length() ? text.charAt(column) : ' ';
                walls[row][column] = tile == '#';
                empty[row][column] = tile == ' ';
                if (empty[row][column] && (row == 0 || row == rowCount - 1 || column == 0 || column == columnCount - 1)) {
                    // Mark when queued so each cell is scheduled at most once.
                    exterior[row][column] = true;
                    pending.add(row * columnCount + column);
                }
            }
        }

        // Flood from outside, not from the player: sealed empty rooms stay wood.
        while (!pending.isEmpty()) {
            int position = pending.removeFirst();
            // Recover the coordinates and expand through four neighbors, never diagonals.
            int row = position / columnCount;
            int column = position % columnCount;
            visit(row - 1, column, empty, pending);
            visit(row + 1, column, empty, pending);
            visit(row, column - 1, empty, pending);
            visit(row, column + 1, empty, pending);
        }
    }

    /** Height of the source layout, including any empty rows. */
    int getRowCount() {
        return rowCount;
    }

    /** Width of the bounding rectangle, including padding after shorter rows. */
    int getColumnCount() {
        return columnCount;
    }

    /** Beyond the bounding rectangle is also outdoors for drawing purposes. */
    boolean isExterior(int row, int column) {
        return !contains(row, column) || exterior[row][column];
    }

    /** Only an actual # in the saved board is a wall, including at the edges. */
    boolean isWall(int row, int column) {
        return contains(row, column) && walls[row][column];
    }

    /** Guard mask access before indexing either array dimension. */
    private boolean contains(int row, int column) {
        return row >= 0 && row < rowCount && column >= 0 && column < columnCount;
    }

    /** Add a previously unseen blank neighbor to the breadth-first search. */
    private void visit(int row, int column, boolean[][] empty, ArrayDeque<Integer> pending) {
        // Short-circuit checks keep out-of-range array accesses from being evaluated.
        if (contains(row, column) && empty[row][column] && !exterior[row][column]) {
            exterior[row][column] = true;
            pending.addLast(row * columnCount + column);
        }
    }
}
