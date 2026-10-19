package sokoban;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The rules of Sokoban as pure functions on a board.
 *
 * <p>A board is a list of strings, one per row, using the symbols in {@link Tiles}.
 * Nothing here changes the board it is given: a move returns a brand-new board.
 */
public final class BoardRules {
    private BoardRules() {
        // Only static methods: there is nothing to instantiate.
    }

    /** A row/column pair used while searching a board for the player. */
    private record Position(int row, int column) { }

    /**
     * Try to move the player one square.
     *
     * @param rows the current board
     * @param dr   rows to move (-1 up, 1 down, 0 sideways)
     * @param dc   columns to move (-1 left, 1 right, 0 vertically)
     * @return the new board, or empty if the move is illegal or the board has no player
     */
    public static Optional<List<String>> tryMove(List<String> rows, int dr, int dc) {
        if (!isSingleStep(dr, dc)) {
            return Optional.empty();
        }
        Position player = findPlayer(rows);
        if (player == null) {
            return Optional.empty();
        }
        return movePlayer(rows, player, dr, dc);
    }

    /** True only for one square straight up, down, left or right (never diagonal or zero). */
    private static boolean isSingleStep(int dr, int dc) {
        boolean verticalStep = (dr == -1 || dr == 1) && dc == 0;
        boolean horizontalStep = (dc == -1 || dc == 1) && dr == 0;
        return verticalStep || horizontalStep;
    }

    /** Scan the board row by row for the player; returns null if there is none. */
    private static Position findPlayer(List<String> rows) {
        for (int row = 0; row < rows.size(); row++) {
            String text = rows.get(row);
            for (int column = 0; column < text.length(); column++) {
                if (Tiles.isPlayer(text.charAt(column))) {
                    return new Position(row, column);
                }
            }
        }
        return null;
    }

    /** Apply one step for a player who is known to be at {@code player}. */
    private static Optional<List<String>> movePlayer(
            List<String> rows, Position player, int dr, int dc) {
        int row = player.row();
        int column = player.column();
        int nextRow = row + dr;
        int nextColumn = column + dc;
        char nextTile = tileAt(rows, nextRow, nextColumn);

        // If a box is in the way, the player pushes it one square further along.
        boolean pushingBox = Tiles.isBox(nextTile);
        int boxRow = nextRow + dr;
        int boxColumn = nextColumn + dc;

        // Walking needs the next square free; pushing needs the square beyond the box free.
        char destination = pushingBox ? tileAt(rows, boxRow, boxColumn) : nextTile;
        if (!Tiles.isOpenFloor(destination)) {
            return Optional.empty();
        }

        // Every check has passed, so it is now safe to build the changed copy of the board.
        List<String> moved = new ArrayList<>(rows);
        if (pushingBox) {
            char pushedBox = destination == Tiles.GOAL ? Tiles.BOX_ON_GOAL : Tiles.BOX;
            setTile(moved, boxRow, boxColumn, pushedBox);
        }

        // The square the player leaves shows a goal again if they were standing on one.
        boolean leavingGoal = tileAt(rows, row, column) == Tiles.PLAYER_ON_GOAL;
        setTile(moved, row, column, leavingGoal ? Tiles.GOAL : Tiles.FLOOR);

        // A box pushed off a goal leaves that goal beneath the arriving player.
        boolean arrivingOnGoal = nextTile == Tiles.GOAL || nextTile == Tiles.BOX_ON_GOAL;
        setTile(moved, nextRow, nextColumn, arrivingOnGoal ? Tiles.PLAYER_ON_GOAL : Tiles.PLAYER);
        return Optional.of(List.copyOf(moved));
    }

    /** Read a square, treating anything off the board or past the end of a short row as a wall. */
    private static char tileAt(List<String> rows, int row, int column) {
        if (row < 0 || row >= rows.size() || column < 0 || column >= rows.get(row).length()) {
            return Tiles.WALL;
        }
        return rows.get(row).charAt(column);
    }

    /** Strings cannot be edited, so replace the whole row with a changed copy. */
    private static void setTile(List<String> rows, int row, int column, char tile) {
        char[] tiles = rows.get(row).toCharArray();
        tiles[column] = tile;
        rows.set(row, new String(tiles));
    }

    /**
     * The win condition: at least one box exists and every box is on a goal.
     * A board with no boxes at all is not a win.
     */
    public static boolean isSolved(List<String> rows) {
        boolean foundBoxOnGoal = false;
        for (String row : rows) {
            for (int column = 0; column < row.length(); column++) {
                char tile = row.charAt(column);
                if (tile == Tiles.BOX) {
                    return false; // A box that is not on a goal means the level is unfinished.
                }
                if (tile == Tiles.BOX_ON_GOAL) {
                    foundBoxOnGoal = true;
                }
            }
        }
        return foundBoxOnGoal;
    }

    /**
     * The length of the longest row. Level rows can have different lengths, so code
     * that needs a rectangular grid sizes it with this.
     */
    public static int widestRowLength(List<String> rows) {
        int widest = 0;
        for (String row : rows) {
            widest = Math.max(widest, row.length());
        }
        return widest;
    }
}
