package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Exercise scoring and completion through real movement rather than UI key presses. */
class PlayerProgressTest {
    @Test
    void onlySuccessfulMovesCostPointsIncludingPushes() {
        State state = new State(List.of(List.of("#######", "#@ $ .#", "#######")));
        GameLogic game = new GameLogic(state);
        assertEquals(10_000, state.getScore());

        game.moveCharacter(-1, 0); // Wall.
        game.moveCharacter(1, 1); // Diagonal.
        assertEquals(0, state.getMoveCount());
        assertEquals(10_000, state.getScore());

        game.moveCharacter(0, 1); // Walk.
        game.moveCharacter(0, 1); // Push onto floor.
        assertEquals(2, state.getMoveCount());
        assertEquals(9_900, state.getScore());
        assertFalse(game.isAttemptCompleted());

        State blocked = new State(List.of(List.of("#@$#")));
        new GameLogic(blocked).moveCharacter(0, 1);
        assertEquals(0, blocked.getMoveCount());
    }

    @Test
    void finalPushRecordsResultAndFreezesAttemptUntilRestart() {
        State state = new State(List.of(List.of("#######", "#@ $ .#", "#######")));
        PlayerProgress progress = new PlayerProgress();
        GameLogic game = new GameLogic(state, progress, "original");
        for (int step = 0; step < 3; step++) {
            game.moveCharacter(0, 1);
        }
        assertTrue(state.isSolved());
        assertTrue(game.isAttemptCompleted());
        assertEquals(new PlayerRecord(9_850, 3), progress.getRecord("original:1"));

        game.moveCharacter(0, -1);
        assertEquals(3, state.getMoveCount());
        assertEquals('@', state.getTile(1, 4));

        game.selectLevel(0);
        assertFalse(game.isAttemptCompleted());
        assertEquals(0, state.getMoveCount());
        assertEquals(10_000, state.getScore());
        assertTrue(progress.isCompleted("original:1"));
        game.moveCharacter(0, 1);
        assertEquals(1, state.getMoveCount());
    }

    @Test
    void switchingLevelsResetsAttemptAndSeparatesRecords() {
        List<String> level = List.of("#####", "#@$.#", "#####");
        State state = new State(List.of(level, level));
        PlayerProgress progress = new PlayerProgress();
        GameLogic original = new GameLogic(state, progress, "original");
        original.moveCharacter(0, 1);
        original.selectLevel(1);
        assertEquals(0, state.getMoveCount());
        assertEquals(10_000, state.getScore());
        assertFalse(original.isAttemptCompleted());
        assertTrue(progress.isCompleted("original:1"));
        assertFalse(progress.isCompleted("original:2"));

        GameLogic microban = new GameLogic(new State(List.of(level)), progress, "microban");
        microban.moveCharacter(0, 1);
        assertTrue(progress.isCompleted("microban:1"));
        assertEquals(2, progress.getRecords().size());
    }

    @Test
    void scoreStopsAtZeroButZeroPointCompletionsStillCount() {
        State state = new State(List.of(List.of("#######", "#@  $.#", "#######")));
        GameLogic game = new GameLogic(state);
        // Waste 200 valid steps without touching the box.
        for (int step = 0; step < 100; step++) {
            game.moveCharacter(0, 1);
            game.moveCharacter(0, -1);
        }
        assertEquals(0, state.getScore());
        game.moveCharacter(0, 1);
        game.moveCharacter(0, 1);
        game.moveCharacter(0, 1);
        assertEquals(203, state.getMoveCount());
        assertEquals(new PlayerRecord(0, 203), game.getProgress().getRecord("default:1"));
    }

    @Test
    void replayOnlyImprovesBestsAndSnapshotsCannotChangeRecords() {
        PlayerProgress progress = new PlayerProgress();
        progress.recordCompletion("original:1", 8_500, 30);
        progress.recordCompletion("original:1", 8_000, 40);
        assertEquals(new PlayerRecord(8_500, 30), progress.getRecord("original:1"));
        progress.recordCompletion("original:1", 9_000, 20);
        assertEquals(new PlayerRecord(9_000, 20), progress.getRecord("original:1"));
        assertThrows(UnsupportedOperationException.class, () -> progress.getRecords().clear());
    }

    @Test
    void emptyBoardsAndUnsolvedBoxesAreNotWins() {
        assertFalse(new State(List.of(List.of("#@ #"))).isSolved());
        assertFalse(new State(List.of(List.of("#@*$#"))).isSolved());
        assertTrue(new State(List.of(List.of("#@**#"))).isSolved());
    }
}
