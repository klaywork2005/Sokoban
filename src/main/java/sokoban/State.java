package sokoban;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The shared game data: the saved levels, which one is selected, the board as it
 * is being played, and the move count for the current attempt.
 *
 * <p>Every board the player has moved away from is remembered, so a move can be undone.
 * The move count is simply the number of remembered boards, so taking a move back also
 * takes back its cost.
 *
 * <p>Besides the saved levels there can be one extra "generated" level, made by
 * {@link LevelGenerator}. It is listed after the saved levels, and making a new one replaces it.
 *
 * <p>Create exactly one instance in {@code Main} and pass it to everything that needs it.
 * This class knows nothing about Swing, files, or how the board is drawn.
 */
public final class State {
    /** Every attempt starts with this many points. */
    public static final int STARTING_SCORE = 500_000;
    /** Points lost for each successful move, including a push. */
    public static final int MOVE_PENALTY = 50;

    // The saved levels never change; the separate board below changes as the game is played.
    private final List<List<String>> levels;
    // The level made by the generator, or null until the player asks for one.
    private List<String> generatedLevel;
    private int currentLevelIndex;
    private char[][] board;
    // The boards before each successful move, newest first. A Deque is a list that is
    // quick to add to and remove from either end; here we only use the front, like a stack
    // of plates. Only moves are stored (as boards); the score is calculated from how many
    // there are, so the two cannot disagree.
    private final Deque<List<String>> history = new ArrayDeque<>();

    /**
     * Validate and keep a private copy of the levels, then select the first one.
     *
     * @throws IllegalArgumentException if there are no levels or a level has no board
     */
    public State(List<List<String>> levels) {
        if (levels.isEmpty()) {
            throw new IllegalArgumentException("At least one level is required.");
        }
        // Copy every level so later changes by the caller cannot alter the saved layouts.
        List<List<String>> savedLevels = new ArrayList<>();
        for (List<String> level : levels) {
            if (!containsBoard(level)) {
                throw new IllegalArgumentException("Levels must contain a board.");
            }
            savedLevels.add(List.copyOf(level)); // copyOf makes a read-only copy.
        }
        this.levels = List.copyOf(savedLevels);

        currentLevelIndex = 0;
        resetBoard();
    }

    /** A level needs at least one row with something other than spaces to be worth showing. */
    private static boolean containsBoard(List<String> level) {
        for (String row : level) {
            if (!row.isBlank()) {
                return true;
            }
        }
        return false;
    }

    /** Number of levels for the level picker, including the generated one if there is one. */
    public int getLevelCount() {
        if (generatedLevel == null) {
            return levels.size();
        }
        return levels.size() + 1;
    }

    /** Number of levels that came from the level file, not counting the generated one. */
    public int getSavedLevelCount() {
        return levels.size();
    }

    /** True if the level at this index is the generated one, which comes after the saved levels. */
    public boolean isGeneratedLevel(int index) {
        return generatedLevel != null && index == levels.size();
    }

    /** Indices start at 0; add 1 when showing a level number to the player. */
    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }

    /** The saved rows of the selected level. These never change, even as the board does. */
    public List<String> getCurrentLevel() {
        if (isGeneratedLevel(currentLevelIndex)) {
            return generatedLevel;
        }
        return levels.get(currentLevelIndex);
    }

    /**
     * Select a level by its zero-based index and throw away all moves made on the old board.
     *
     * @throws IllegalArgumentException if the index is outside the available levels
     */
    public void setCurrentLevelIndex(int index) {
        // Check first so a bad index leaves both the selection and the board untouched.
        if (index < 0 || index >= getLevelCount()) {
            throw new IllegalArgumentException("Invalid level index: " + index);
        }
        currentLevelIndex = index;
        resetBoard();
    }

    /**
     * Keep this as the generated level, replacing any earlier one, and start playing it.
     *
     * @throws IllegalArgumentException if the rows do not contain a board
     */
    public void setGeneratedLevel(List<String> rows) {
        if (!containsBoard(rows)) {
            throw new IllegalArgumentException("Levels must contain a board.");
        }
        generatedLevel = List.copyOf(rows);
        // The generated level always sits right after the saved ones.
        setCurrentLevelIndex(levels.size());
    }

    /** Start the selected level over: fresh board from the saved rows and zero moves. */
    private void resetBoard() {
        history.clear();
        board = toTileArray(getCurrentLevel());
    }

    /** Turn rows of text into an editable grid. Each row keeps its own length. */
    private static char[][] toTileArray(List<String> rows) {
        char[][] tiles = new char[rows.size()][];
        for (int row = 0; row < rows.size(); row++) {
            tiles[row] = rows.get(row).toCharArray();
        }
        return tiles;
    }

    /** Height of the board being played. */
    public int getRowCount() {
        return board.length;
    }

    /** Width of one row. Rows can differ in length, so callers must pass a valid row index. */
    public int getColumnCount(int row) {
        return board[row].length;
    }

    /** Read one square; anything off the board or past the end of a short row is a wall. */
    public char getTile(int row, int column) {
        if (row < 0 || row >= board.length || column < 0 || column >= board[row].length) {
            return Tiles.WALL;
        }
        return board[row][column];
    }

    /** Successful steps in this attempt, including pushes. Steps that were undone do not count. */
    public int getMoveCount() {
        return history.size();
    }

    /** The starting score minus a penalty per move, never dropping below zero. */
    public int getScore() {
        // Use long arithmetic so a huge move count cannot overflow before we clamp it.
        long score = STARTING_SCORE - (long) getMoveCount() * MOVE_PENALTY;
        if (score < 0) {
            return 0;
        }
        return (int) score;
    }

    /** A copy of the board as it is right now; changing the copy does not affect the game. */
    public List<String> getBoardSnapshot() {
        List<String> rows = new ArrayList<>();
        for (char[] tiles : board) {
            rows.add(new String(tiles));
        }
        return List.copyOf(rows);
    }

    /**
     * Make a move: replace the board with a position that {@link BoardRules} has already
     * approved, and remember the old board so the move can be undone. This is the only way
     * the move count goes up.
     */
    void applyMove(List<String> rows) {
        history.push(getBoardSnapshot()); // push adds to the front of the deque.
        board = toTileArray(rows);
    }

    /** True if at least one move can be taken back. */
    public boolean canUndo() {
        return !history.isEmpty();
    }

    /**
     * Go back to the board as it was before the last move.
     *
     * @return true if a move was undone, false if there was nothing to undo
     */
    boolean undoMove() {
        if (history.isEmpty()) {
            return false;
        }
        board = toTileArray(history.pop()); // pop removes the newest board from the front.
        return true;
    }

    /** True when the board has boxes and every one of them is on a goal. */
    public boolean isSolved() {
        return BoardRules.isSolved(getBoardSnapshot());
    }
}
