package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Check that the difficulty settings make sense and get harder from one step to the next. */
class DifficultyTest {
    @Test
    void thePlayerSeesFriendlyNames() {
        // A drop-down list shows toString(), so these are what appear on screen.
        assertEquals("Easy", Difficulty.EASY.toString());
        assertEquals("Medium", Difficulty.MEDIUM.toString());
        assertEquals("Hard", Difficulty.HARD.toString());
    }

    @Test
    void everySettingIsAcceptedByTheGenerator() {
        for (Difficulty difficulty : Difficulty.values()) {
            int roomSquares = (difficulty.width() - 2) * (difficulty.height() - 2);

            // The generator needs a room of at least 5 by 5 with space for every box,
            // the player, and the extra walls.
            assertTrue(difficulty.width() >= 5, difficulty + " is too narrow.");
            assertTrue(difficulty.height() >= 5, difficulty + " is too short.");
            assertTrue(difficulty.boxes() >= 1, difficulty + " needs a box.");
            assertTrue(difficulty.pulls() >= 1, difficulty + " needs some pulls.");
            assertTrue(difficulty.boxes() + 1 + difficulty.walls() <= roomSquares,
                    difficulty + " has no room for everything.");
        }
    }

    @Test
    void eachStepHasMoreBoxesAndMoreMixingThanTheLast() {
        assertTrue(Difficulty.EASY.boxes() < Difficulty.MEDIUM.boxes());
        assertTrue(Difficulty.MEDIUM.boxes() < Difficulty.HARD.boxes());
        assertTrue(Difficulty.EASY.pulls() < Difficulty.MEDIUM.pulls());
        assertTrue(Difficulty.MEDIUM.pulls() < Difficulty.HARD.pulls());
    }
}
