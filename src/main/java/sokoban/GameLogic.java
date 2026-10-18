package sokoban;

/** Handles movement and box pushing. This class does not use Swing. */
public final class GameLogic {
    // This is the same board the window and renderer use, not a separate game.
    private final State state;
    // Cache the player's zero-based position so each move avoids scanning the board.
    private int row;
    private int column;

    /** Attach the rules to an existing state and locate its starting player. */
    public GameLogic(State state) {
        this.state = state;
        findPlayer();
    }

    /** Load a fresh board, even when selecting the level that is already active. */
    public void selectLevel(int index) {
        state.setCurrentLevelIndex(index);
        findPlayer();
    }

    /** Find either a player on floor (@) or a player on a goal (+). */
    private void findPlayer() {
        // Keep the sentinel values if the level contains no player; moves then do nothing.
        row = -1;
        column = -1;
        for (int r = 0; r < state.getRowCount(); r++) {
            for (int c = 0; c < state.getColumnCount(r); c++) {
                char tile = state.getTile(r, c);
                if (tile == '@' || tile == '+') {
                    row = r;
                    column = c;
                    return;
                }
            }
        }
    }

    /**
     * Attempt one orthogonal step: dr changes the row and dc changes the column.
     * Negative rows move up; negative columns move left. Blocked moves do nothing.
     */
    public void moveCharacter(int dr, int dc) {
        // Only one-square horizontal or vertical moves are valid.
        if (Math.abs(dr) + Math.abs(dc) != 1 || row == -1) {
            return;
        }

        // Out-of-bounds coordinates read as walls through State.getTile().
        int nextRow = row + dr;
        int nextColumn = column + dc;
        char nextTile = state.getTile(nextRow, nextColumn);

        if (nextTile == '$' || nextTile == '*') {
            // A push moves the box one square farther in the same direction.
            int boxRow = nextRow + dr;
            int boxColumn = nextColumn + dc;
            char destination = state.getTile(boxRow, boxColumn);

            // A box cannot push another box or move through a wall.
            if (destination != ' ' && destination != '.') {
                return;
            }

            // The combined symbols preserve whether a goal lies under the box.
            if (destination == '.') {
                state.setTile(boxRow, boxColumn, '*');
            } else {
                state.setTile(boxRow, boxColumn, '$');
            }

            // Leave the goal in place when a box moves off it.
            if (nextTile == '*') {
                state.setTile(nextRow, nextColumn, '.');
            } else {
                state.setTile(nextRow, nextColumn, ' ');
            }
        } else if (nextTile != ' ' && nextTile != '.') {
            // Without a box to push, only bare floor or an uncovered goal is walkable.
            return;
        }

        // Restore the terrain beneath the player's previous position.
        if (state.getTile(row, column) == '+') {
            state.setTile(row, column, '.');
        } else {
            state.setTile(row, column, ' ');
        }

        // After a push, this square now contains the terrain the box was covering.
        if (state.getTile(nextRow, nextColumn) == '.') {
            state.setTile(nextRow, nextColumn, '+');
        } else {
            state.setTile(nextRow, nextColumn, '@');
        }
        // Update the cached coordinates only after the board changes succeed.
        row = nextRow;
        column = nextColumn;
    }
}
