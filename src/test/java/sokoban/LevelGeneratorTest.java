package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Check that generated levels are well formed and, above all, that they can be solved. */
class LevelGeneratorTest {
    private static LevelGenerator generatorWithSeed(long seed) {
        // A fixed seed makes the "random" choices repeat, so a failing test can be run again.
        return new LevelGenerator(new Random(seed));
    }

    /** Count how many times one symbol appears on the whole board. */
    private static int count(List<String> rows, char symbol) {
        int total = 0;
        for (String row : rows) {
            for (int column = 0; column < row.length(); column++) {
                if (row.charAt(column) == symbol) {
                    total++;
                }
            }
        }
        return total;
    }

    @Test
    void everyGeneratedLevelCanBeSolvedByReplayingItsSolution() {
        for (long seed = 1; seed <= 200; seed++) {
            LevelGenerator.GeneratedLevel level = generatorWithSeed(seed).generate();

            // Play the solution on the real game rules, exactly as a person would.
            State state = new State(List.of(level.rows()));
            GameLogic game = new GameLogic(state);
            for (Direction step : level.solution()) {
                game.moveCharacter(step.dr(), step.dc());
            }

            assertTrue(game.isAttemptCompleted(), "Seed " + seed + " could not be solved.");
        }
    }

    @Test
    void biggerAndHarderLevelsCanAlsoBeSolved() {
        for (long seed = 1; seed <= 50; seed++) {
            LevelGenerator generator = generatorWithSeed(seed);
            LevelGenerator.GeneratedLevel level = generator.generate(12, 10, 5, 15, 80);

            State state = new State(List.of(level.rows()));
            GameLogic game = new GameLogic(state);
            for (Direction step : level.solution()) {
                game.moveCharacter(step.dr(), step.dc());
            }

            assertTrue(game.isAttemptCompleted(), "Seed " + seed + " could not be solved.");
        }
    }

    @Test
    void levelHasTheRequestedNumberOfBoxesAndGoalsAndOnePlayer() {
        List<String> rows = generatorWithSeed(7).generate(9, 9, 4, 6, 30).rows();

        // A box on a goal (*) counts as both a box and a goal, and so does a player on a goal (+).
        int boxes = count(rows, Tiles.BOX) + count(rows, Tiles.BOX_ON_GOAL);
        int goals = count(rows, Tiles.GOAL) + count(rows, Tiles.BOX_ON_GOAL)
                + count(rows, Tiles.PLAYER_ON_GOAL);
        int players = count(rows, Tiles.PLAYER) + count(rows, Tiles.PLAYER_ON_GOAL);

        assertEquals(4, boxes);
        assertEquals(4, goals);
        assertEquals(1, players);
    }

    @Test
    void levelHasTheRequestedSizeAndIsSurroundedByWalls() {
        List<String> rows = generatorWithSeed(3).generate(10, 8, 3, 5, 20).rows();

        assertEquals(8, rows.size());
        for (int row = 0; row < rows.size(); row++) {
            String text = rows.get(row);
            assertEquals(10, text.length());
            assertEquals(Tiles.WALL, text.charAt(0));
            assertEquals(Tiles.WALL, text.charAt(9));
            if (row == 0 || row == rows.size() - 1) {
                assertEquals("#".repeat(10), text);
            }
        }
    }

    @Test
    void levelIsNeverAlreadySolvedAtTheStart() {
        for (long seed = 1; seed <= 200; seed++) {
            List<String> rows = generatorWithSeed(seed).generate().rows();
            assertFalse(BoardRules.isSolved(rows), "Seed " + seed + " starts out solved.");
        }
    }

    @Test
    void mostBoxesStartAwayFromTheirGoalsAndTheSolutionIsNotTrivial() {
        for (long seed = 1; seed <= 200; seed++) {
            LevelGenerator.GeneratedLevel level = generatorWithSeed(seed).generate();

            int boxesOffGoals = count(level.rows(), Tiles.BOX);
            assertTrue(boxesOffGoals * 2 >= 3, "Seed " + seed + " has too few boxes to move.");
            assertTrue(level.solution().size() >= 12, "Seed " + seed + " is too easy.");
        }
    }

    @Test
    void theSameSeedMakesTheSameLevel() {
        LevelGenerator.GeneratedLevel first = generatorWithSeed(42).generate();
        LevelGenerator.GeneratedLevel second = generatorWithSeed(42).generate();

        assertEquals(first.rows(), second.rows());
        assertEquals(first.solution(), second.solution());
    }

    @Test
    void differentSeedsMakeDifferentLevels() {
        Set<List<String>> levels = new HashSet<>();
        for (long seed = 1; seed <= 10; seed++) {
            levels.add(generatorWithSeed(seed).generate().rows());
        }

        // Ten random levels being all the same would mean the randomness is not working.
        assertTrue(levels.size() > 5);
    }

    @Test
    void settingsThatCannotMakeALevelAreRejected() {
        LevelGenerator generator = generatorWithSeed(1);

        assertThrows(IllegalArgumentException.class, () -> generator.generate(4, 9, 3, 5, 20));
        assertThrows(IllegalArgumentException.class, () -> generator.generate(9, 4, 3, 5, 20));
        assertThrows(IllegalArgumentException.class, () -> generator.generate(9, 9, 0, 5, 20));
        assertThrows(IllegalArgumentException.class, () -> generator.generate(9, 9, 3, -1, 20));
        assertThrows(IllegalArgumentException.class, () -> generator.generate(9, 9, 3, 5, 0));
        // A 5 by 5 room has only 9 floor squares, so 9 boxes leave no space for the player.
        assertThrows(IllegalArgumentException.class, () -> generator.generate(5, 5, 9, 0, 20));
    }
}
