package sokoban;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds boxes that can never reach a goal again, so the game can tell the player they are stuck.
 *
 * <p>Two kinds of stuck box are found:
 * <ul>
 *   <li><b>A box on a dead square.</b> A dead square is one where a box can never be pushed to
 *       any goal, whatever else happens. A corner is the obvious example, but so is a square
 *       along a wall that has no goal in line with it.</li>
 *   <li><b>A box in a frozen block.</b> If a box is part of a 2 by 2 block of walls and boxes,
 *       none of them can ever be pushed, because there is never room to stand behind one.</li>
 * </ul>
 *
 * <p>A warning from here is always right: a box reported as stuck really cannot be saved. The
 * tests check this by replaying known solutions and making sure no position along the way is
 * ever reported. The reverse is not true. Some stuck positions need cleverer checks, so "not
 * stuck" only means "no stuck box was found".
 */
public final class DeadlockDetector {
    private DeadlockDetector() {
        // Only static methods: there is nothing to instantiate.
    }

    /** A spot on the board. */
    public record Square(int row, int column) { }

    /** True if at least one box on the board is stuck for good. */
    public static boolean hasStuckBox(List<String> board) {
        return !findStuckBoxes(board).isEmpty();
    }

    /**
     * Find every box that is stuck for good. A box that is already on a goal is never
     * reported, even when it is part of a frozen block, because it is already where it belongs.
     *
     * @param board the board as rows of level symbols
     * @return the squares of the stuck boxes, in reading order (top to bottom, left to right)
     */
    public static List<Square> findStuckBoxes(List<String> board) {
        boolean[][] live = findLiveSquares(board);

        List<Square> stuck = new ArrayList<>();
        for (int row = 0; row < board.size(); row++) {
            for (int column = 0; column < board.get(row).length(); column++) {
                // Only a box that is off its goal ($) can be a problem.
                if (tileAt(board, row, column) != Tiles.BOX) {
                    continue;
                }
                if (!live[row][column] || isInFrozenBlock(board, row, column)) {
                    stuck.add(new Square(row, column));
                }
            }
        }
        return stuck;
    }

    // ---------------------------------------------------------------------------------
    // Dead squares
    // ---------------------------------------------------------------------------------

    /**
     * Work out which squares are "live": a box standing there could, in the best case, be
     * pushed all the way to a goal. Every other square is dead.
     *
     * <p>This works backwards from the goals, like the level generator does. A goal is live.
     * If a box on square B can be pushed one step onto a live square, then B is live too. A
     * push needs room for the box to land and room for the player to stand behind the box,
     * so only walls matter here. Other boxes are ignored, because they might be moved away.
     */
    private static boolean[][] findLiveSquares(List<String> board) {
        boolean[][] live = new boolean[board.size()][BoardRules.widestRowLength(board)];
        ArrayDeque<Square> queue = new ArrayDeque<>();

        for (int row = 0; row < board.size(); row++) {
            for (int column = 0; column < board.get(row).length(); column++) {
                if (Tiles.isGoal(tileAt(board, row, column))) {
                    live[row][column] = true;
                    queue.add(new Square(row, column));
                }
            }
        }

        // Breadth-first search: take a live square off the queue and look for neighbors that
        // could be pushed onto it.
        while (!queue.isEmpty()) {
            Square target = queue.remove();
            for (Direction direction : Direction.values()) {
                // A box pushed in this direction lands on target, so it started one step back
                // from target, with the player standing one step further back still.
                int boxRow = target.row() - direction.dr();
                int boxColumn = target.column() - direction.dc();
                int playerRow = boxRow - direction.dr();
                int playerColumn = boxColumn - direction.dc();

                boolean roomForBox = !isWall(board, boxRow, boxColumn);
                boolean roomForPlayer = !isWall(board, playerRow, playerColumn);
                if (roomForBox && roomForPlayer && !live[boxRow][boxColumn]) {
                    live[boxRow][boxColumn] = true;
                    queue.add(new Square(boxRow, boxColumn));
                }
            }
        }
        return live;
    }

    // ---------------------------------------------------------------------------------
    // Frozen blocks
    // ---------------------------------------------------------------------------------

    /**
     * True if this square belongs to a 2 by 2 block where every square is a wall or a box.
     * A square is part of four different 2 by 2 blocks (it can be any of the four corners),
     * so we check them all.
     */
    private static boolean isInFrozenBlock(List<String> board, int row, int column) {
        // (rowShift, columnShift) is where the block's top-left corner is, relative to us.
        for (int rowShift = -1; rowShift <= 0; rowShift++) {
            for (int columnShift = -1; columnShift <= 0; columnShift++) {
                int top = row + rowShift;
                int left = column + columnShift;
                if (isBlocked(board, top, left) && isBlocked(board, top, left + 1)
                        && isBlocked(board, top + 1, left) && isBlocked(board, top + 1, left + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** True if the square is a wall or holds a box, so nothing can be pushed into it. */
    private static boolean isBlocked(List<String> board, int row, int column) {
        return isWall(board, row, column) || Tiles.isBox(tileAt(board, row, column));
    }

    // ---------------------------------------------------------------------------------
    // Reading the board
    // ---------------------------------------------------------------------------------

    /** True for a wall, or for anything off the board or past the end of a short row. */
    private static boolean isWall(List<String> board, int row, int column) {
        return tileAt(board, row, column) == Tiles.WALL;
    }

    private static char tileAt(List<String> board, int row, int column) {
        if (row < 0 || row >= board.size() || column < 0 || column >= board.get(row).length()) {
            return Tiles.WALL;
        }
        return board.get(row).charAt(column);
    }
}
