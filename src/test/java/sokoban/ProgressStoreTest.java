package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Test real save files in an isolated folder, never in the user's actual profile. */
class ProgressStoreTest {
    @TempDir
    Path directory;

    @Test
    void firstLaunchNeedsNoExistingFileOrDirectory() throws IOException {
        Path file = directory.resolve("new-profile/progress.properties");
        assertTrue(new ProgressStore(file).load().getRecords().isEmpty());
        assertFalse(Files.exists(file));
    }

    @Test
    void completedLevelSurvivesSavingAndStartingAnotherGame() throws IOException {
        ProgressStore store = new ProgressStore(directory.resolve("profile/progress.properties"));
        List<List<String>> levels = List.of(List.of("#####", "#@$.#", "#####"));
        GameLogic game = new GameLogic(new State(levels), store.load(), "/original-levels.txt");
        game.moveCharacter(0, 1);
        game.getProgress().recordCompletion("microban:1", 0, 250);
        store.save(game.getProgress());

        PlayerProgress loaded = store.load();
        int scoreAfterOneMove = State.STARTING_SCORE - State.MOVE_PENALTY;
        assertEquals(new PlayerRecord(scoreAfterOneMove, 1),
                loaded.getRecord("/original-levels.txt:1"));
        assertTrue(loaded.isCompleted("microban:1"));
        assertEquals(new PlayerRecord(0, 250), loaded.getRecord("microban:1"));
        State restarted = new State(levels);
        GameLogic nextGame = new GameLogic(restarted, loaded, "/original-levels.txt");
        assertEquals(0, restarted.getMoveCount());
        assertEquals(State.STARTING_SCORE, restarted.getScore());
        assertFalse(nextGame.isAttemptCompleted());

        // A second save must replace an existing file and retain the other collection.
        loaded.recordCompletion("microban:1", 9_000, 20);
        store.save(loaded);
        assertEquals(loaded.getRecords(), store.load().getRecords());
        try (var files = Files.list(directory.resolve("profile"))) {
            assertEquals(List.of("progress.properties"),
                    files.map(path -> path.getFileName().toString()).toList());
        }
    }

    @Test
    void corruptDataIsReportedWithoutOverwritingTheSave() throws IOException {
        Path file = directory.resolve("progress.properties");
        for (String data : List.of(
                "test.completed=true\ntest.bestScore=invalid\ntest.fewestMoves=10\n",
                "test.completed=true\ntest.bestScore=9000\n",
                "test.completed=true\ntest.bestScore=-50\ntest.fewestMoves=10\n")) {
            Files.writeString(file, data);
            assertThrows(IOException.class, () -> new ProgressStore(file).load());
            assertEquals(data, Files.readString(file));
        }
    }

    @Test
    void saveFailureIsReportedToCaller() throws IOException {
        Path parentFile = directory.resolve("a-file");
        Files.writeString(parentFile, "not a directory");
        ProgressStore store = new ProgressStore(parentFile.resolve("progress.properties"));
        assertThrows(IOException.class, () -> store.save(new PlayerProgress()));
        assertEquals("not a directory", Files.readString(parentFile));
    }
}
