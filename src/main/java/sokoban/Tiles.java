package sokoban;

/**
 * Names for the characters used in Sokoban (XSB) level files and on the live board.
 *
 * <p>Using {@code Tiles.BOX} instead of the bare character {@code '$'} makes the
 * rules and drawing code read like English, and keeps the symbols defined in one place.
 */
public final class Tiles {
    /** A wall: nothing can enter this square. */
    public static final char WALL = '#';
    /** Bare floor with nothing on it. */
    public static final char FLOOR = ' ';
    /** A goal square with nothing on it. */
    public static final char GOAL = '.';
    /** A box standing on ordinary floor. */
    public static final char BOX = '$';
    /** A box standing on a goal; this is what a finished level is full of. */
    public static final char BOX_ON_GOAL = '*';
    /** The player standing on ordinary floor. */
    public static final char PLAYER = '@';
    /** The player standing on a goal. */
    public static final char PLAYER_ON_GOAL = '+';

    // Every character a level file may contain, used to validate input.
    private static final String VALID_TILES = " #.$@*+";

    private Tiles() {
        // Only static members: there is nothing to instantiate.
    }

    /** True for any character that may appear in a level file. */
    public static boolean isValid(char tile) {
        return VALID_TILES.indexOf(tile) >= 0;
    }

    /** True for a box, whether or not it stands on a goal. */
    public static boolean isBox(char tile) {
        return tile == BOX || tile == BOX_ON_GOAL;
    }

    /** True for the player, whether or not they stand on a goal. */
    public static boolean isPlayer(char tile) {
        return tile == PLAYER || tile == PLAYER_ON_GOAL;
    }

    /** True for any square that is a goal underneath, even when something covers it. */
    public static boolean isGoal(char tile) {
        return tile == GOAL || tile == BOX_ON_GOAL || tile == PLAYER_ON_GOAL;
    }

    /** True for a square with nothing on it that a box or the player may move onto. */
    public static boolean isOpenFloor(char tile) {
        return tile == FLOOR || tile == GOAL;
    }

    /** True for every valid square except walls; boxes and the player sit on these. */
    public static boolean isInsideRoom(char tile) {
        return isValid(tile) && tile != WALL;
    }
}
