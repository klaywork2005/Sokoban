package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Check that stuck boxes are found, and above all that boxes which can be saved never are. */
class DeadlockDetectorTest {
    private static DeadlockDetector.Square square(int row, int column) {
        return new DeadlockDetector.Square(row, column);
    }

    @Test
    void aBoxInACornerIsStuck() {
        List<String> board = List.of(
                "######",
                "#$  .#",
                "#  @ #",
                "######");

        // The box is in the top-left corner, so it can never be pushed out again.
        assertEquals(List.of(square(1, 1)), DeadlockDetector.findStuckBoxes(board));
        assertTrue(DeadlockDetector.hasStuckBox(board));
    }

    @Test
    void aBoxAlongAWallWithNoGoalOnThatWallIsStuck() {
        List<String> board = List.of(
                "#######",
                "#  $  #",
                "#     #",
                "# @ . #",
                "#######");

        // It can slide along the top wall but never leave it, and the goal is not up there.
        assertEquals(List.of(square(1, 3)), DeadlockDetector.findStuckBoxes(board));
    }

    @Test
    void aBoxAlongAWallIsFineWhenAGoalIsOnThatWall() {
        List<String> board = List.of(
                "#######",
                "#  $ .#",
                "#     #",
                "# @   #",
                "#######");

        assertFalse(DeadlockDetector.hasStuckBox(board));
    }

    @Test
    void aBoxInTheOpenWithAGoalToReachIsNotStuck() {
        List<String> board = List.of(
                "#######",
                "#     #",
                "#  $  #",
                "# @ . #",
                "#     #",
                "#######");

        assertFalse(DeadlockDetector.hasStuckBox(board));
        assertTrue(DeadlockDetector.findStuckBoxes(board).isEmpty());
    }

    @Test
    void aBoxAlreadyOnAGoalIsNeverReportedEvenInACorner() {
        List<String> board = List.of(
                "#####",
                "#*  #",
                "# @ #",
                "#####");

        assertFalse(DeadlockDetector.hasStuckBox(board));
    }

    @Test
    void twoBoxesSideBySideAgainstAWallAreFrozenEvenWithAGoalOnThatWall() {
        List<String> board = List.of(
                "########",
                "# $$ . #",
                "#      #",
                "#  @   #",
                "########");

        // Each box is on a square a lone box could be pushed along, but together they block
        // each other: neither can be pushed sideways, and the wall stops any push downward.
        assertEquals(List.of(square(1, 2), square(1, 3)), DeadlockDetector.findStuckBoxes(board));
    }

    @Test
    void aFrozenBlockOnlyReportsTheBoxesThatAreOffTheirGoals() {
        List<String> board = List.of(
                "#######",
                "#  *$.#",
                "# @   #",
                "#######");

        // The box on the goal is where it should be, but it is what traps its neighbor.
        assertEquals(List.of(square(1, 4)), DeadlockDetector.findStuckBoxes(board));
    }

    @Test
    void twoBoxesBesideEachOtherInTheOpenAreNotFrozen() {
        List<String> board = List.of(
                "########",
                "#      #",
                "#  $$  #",
                "# @ .. #",
                "#      #",
                "########");

        assertFalse(DeadlockDetector.hasStuckBox(board));
    }

    @Test
    void aBoardWithNoBoxesOrASolvedBoardHasNothingStuck() {
        assertFalse(DeadlockDetector.hasStuckBox(List.of("####", "#@.#", "####")));
        assertFalse(DeadlockDetector.hasStuckBox(List.of("#####", "#@ *#", "#####")));
    }

    @Test
    void squaresPastTheEndOfAShortRowCountAsWalls() {
        // The bottom row is shorter than the others. This must not crash.
        List<String> board = List.of(
                "#####",
                "#@$.",
                "###");

        assertFalse(DeadlockDetector.hasStuckBox(board));
    }

    @Test
    void theBoardPassedInIsNotChanged() {
        List<String> board = List.of("######", "#$  .#", "#  @ #", "######");
        List<String> copy = List.copyOf(board);

        DeadlockDetector.findStuckBoxes(board);

        assertEquals(copy, board);
    }

    @Test
    void noPositionOnTheWayToAKnownSolutionIsEverReportedAsStuck() {
        // Following a real solution means every position along it can still be won, so a
        // warning at any of them would be a false alarm.
        for (Difficulty difficulty : Difficulty.values()) {
            for (long seed = 1; seed <= 60; seed++) {
                LevelGenerator generator = new LevelGenerator(new Random(seed));
                LevelGenerator.GeneratedLevel level = generator.generate(difficulty);

                State state = new State(List.of(level.rows()));
                GameLogic game = new GameLogic(state);
                String where = difficulty + ", seed " + seed;
                assertFalse(game.hasStuckBox(), where + " starts out stuck.");
                for (Direction step : level.solution()) {
                    game.moveCharacter(step.dr(), step.dc());
                    assertFalse(game.hasStuckBox(), where + " was reported stuck mid-solution.");
                }
                assertTrue(game.isAttemptCompleted(), where + " was not solved.");
            }
        }
    }
}
