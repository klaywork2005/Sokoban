package sokoban;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared application data. Create one instance in Main and pass it to consumers.
 * This class knows nothing about Swing, files, or how the board is drawn.
 */
public final class State {
    // Saved levels are immutable templates; the separate board changes during play.
    private final List<List<String>> levels;
    private int currentLevelIndex;
    private char[][] board;

    /** Validate and preserve the supplied layouts, then start at the first level. */
    public State(List<List<String>> levels) {
        if (levels.isEmpty()) {
            throw new IllegalArgumentException("At least one level is required.");
        }
        // Copy both lists so callers cannot accidentally change the saved levels.
        List<List<String>> copies = new ArrayList<>();
        for (List<String> level : levels) {
            // An empty or whitespace-only layout has nothing useful to display.
            boolean hasBoard = false;
            for (String row : level) {
                if (!row.isBlank()) {
                    hasBoard = true;
                    break;
                }
            }
            if (!hasBoard) {
                throw new IllegalArgumentException("Levels must contain a board.");
            }
            // copyOf creates a read-only copy, keeping the original layout safe.
            copies.add(List.copyOf(level));
        }
        this.levels = List.copyOf(copies);
        // Initialize a playable copy without changing any of the saved rows.
        currentLevelIndex = 0;
        resetBoard();
    }

    /** Number of saved levels available to the level picker. */
    public int getLevelCount() {
        return levels.size();
    }

    /** Indices start at 0; add 1 when displaying a level number to the player. */
    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }

    /** The returned rows are read-only original level data. */
    public List<String> getCurrentLevel() {
        return levels.get(currentLevelIndex);
    }

    /** Select a zero-based level and discard all moves on the previous board. */
    public void setCurrentLevelIndex(int index) {
        // Reject invalid selections before changing either the index or the board.
        if (index < 0 || index >= levels.size()) {
            throw new IllegalArgumentException("Invalid level index: " + index);
        }
        currentLevelIndex = index;
        resetBoard();
    }

    /** Rebuild mutable rows from the selected level's immutable strings. */
    private void resetBoard() {
        List<String> level = getCurrentLevel();
        board = new char[level.size()][];
        for (int row = 0; row < level.size(); row++) {
            // Keep each row's actual length: XSB boards need not be rectangular.
            board[row] = level.get(row).toCharArray();
        }
    }

    /** Height of the current playable board. */
    public int getRowCount() {
        return board.length;
    }

    /** Width of one existing row; callers must supply a valid row index. */
    public int getColumnCount(int row) {
        return board[row].length;
    }

    /** Read an XSB tile, using a wall to represent any missing square. */
    public char getTile(int row, int column) {
        // Treat missing squares as walls, including short rows at board edges.
        if (row < 0 || row >= board.length
                || column < 0 || column >= board[row].length) {
            return '#';
        }
        return board[row][column];
    }

    /** Write an existing square after the game rules have checked the move. */
    public void setTile(int row, int column, char tile) {
        board[row][column] = tile;
    }
}
