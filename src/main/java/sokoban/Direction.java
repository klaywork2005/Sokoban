package sokoban;

/**
 * The four one-square moves the player can make.
 *
 * <p>Each direction is a pair of offsets: {@code dr} changes the row and {@code dc}
 * changes the column. Rows count downward, so moving up means a smaller row number.
 */
public enum Direction {
    UP(-1, 0),
    DOWN(1, 0),
    LEFT(0, -1),
    RIGHT(0, 1);

    private final int dr;
    private final int dc;

    Direction(int dr, int dc) {
        this.dr = dr;
        this.dc = dc;
    }

    /** How many rows this move changes the player's position by. */
    public int dr() {
        return dr;
    }

    /** How many columns this move changes the player's position by. */
    public int dc() {
        return dc;
    }

    /** The direction that undoes this one: UP gives DOWN, LEFT gives RIGHT, and so on. */
    public Direction opposite() {
        switch (this) {
            case UP:
                return DOWN;
            case DOWN:
                return UP;
            case LEFT:
                return RIGHT;
            default:
                return LEFT; // RIGHT is the only direction left.
        }
    }
}
