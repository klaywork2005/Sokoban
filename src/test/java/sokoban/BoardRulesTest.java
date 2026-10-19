package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Test the board helpers directly. Movement rules are also covered through GameLogicTest. */
class BoardRulesTest {
    @Test
    void widestRowLengthFindsTheLongestRow() {
        assertEquals(7, BoardRules.widestRowLength(List.of("  #####", "  #@ .#", "#####")));
    }

    @Test
    void widestRowLengthIsZeroWithoutRowsOrWithOnlyEmptyRows() {
        assertEquals(0, BoardRules.widestRowLength(List.of()));
        assertEquals(0, BoardRules.widestRowLength(List.of("", "")));
    }

    @Test
    void aMoveReturnsANewBoardAndLeavesTheOriginalAlone() {
        List<String> before = List.of("#@ #");
        List<String> after = BoardRules.tryMove(before, 0, 1).orElseThrow();
        assertEquals(List.of("# @#"), after);
        assertEquals(List.of("#@ #"), before);
    }

    @Test
    void boardsWithoutBoxesAreNotSolved() {
        assertFalse(BoardRules.isSolved(List.of()));
        assertFalse(BoardRules.isSolved(List.of("#@.#")));
    }

    @Test
    void aBoxOffItsGoalKeepsTheBoardUnsolvedEvenIfOthersAreDone() {
        assertFalse(BoardRules.isSolved(List.of("#*$#")));
        assertTrue(BoardRules.isSolved(List.of("#**#")));
    }
}
