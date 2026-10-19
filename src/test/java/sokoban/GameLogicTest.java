package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Exercise movement rules directly against tiny boards, without opening Swing. */
class GameLogicTest {
    // Each fixture uses XSB symbols; assertions read the mutable board after actions.
    @Test
    void playerCanWalkInAllFourDirections() {
        // Put the player in open floor so a four-step loop is unobstructed.
        State state = new State(List.of(List.of("#####", "#   #", "# @ #", "#   #", "#####")));
        GameLogic game = new GameLogic(state);

        // Check each direction and confirm the last vacated square becomes floor.
        game.moveCharacter(-1, 0);
        assertEquals('@', state.getTile(1, 2));
        game.moveCharacter(0, -1);
        assertEquals('@', state.getTile(1, 1));
        game.moveCharacter(1, 0);
        assertEquals('@', state.getTile(2, 1));
        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(2, 2));
        assertEquals(' ', state.getTile(2, 1));
    }

    @Test
    void playerCannotWalkThroughWallsOrMoveDiagonally() {
        State state = new State(List.of(List.of("#####", "#@  #", "#####")));
        GameLogic game = new GameLogic(state);

        // Try a wall, a diagonal, a two-square jump, and a zero-length move.
        game.moveCharacter(-1, 0);
        game.moveCharacter(1, 1);
        game.moveCharacter(0, 2);
        game.moveCharacter(0, 0);

        // All rejected moves must leave the original player and floor untouched.
        assertEquals('@', state.getTile(1, 1));
        assertEquals(' ', state.getTile(1, 2));
    }

    @Test
    void boxMovesOntoAndOffGoalWithoutErasingIt() {
        // A second unsolved box keeps the attempt active while testing a push off a goal.
        State state = new State(List.of(List.of("#######", "#@$.  #", "#$ .  #", "#######")));
        GameLogic game = new GameLogic(state);

        // First push covers the goal with a box (*).
        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(1, 2));
        assertEquals('*', state.getTile(1, 3));

        // Second push leaves the player on the exposed goal (+).
        game.moveCharacter(0, 1);
        assertEquals('+', state.getTile(1, 3));
        assertEquals('$', state.getTile(1, 4));

        // Leaving the goal restores its dot, while the saved template stays unchanged.
        game.moveCharacter(0, -1);
        assertEquals('.', state.getTile(1, 3));
        assertEquals('@', state.getTile(1, 2));
        assertEquals("#@$.  #", state.getCurrentLevel().get(1));
    }

    @Test
    void boxCannotBePushedIntoWallOrAnotherBox() {
        // Separate levels exercise a wall behind the box and a second box behind it.
        State state = new State(List.of(List.of("#@$#"), List.of("#@$$ #")));
        GameLogic game = new GameLogic(state);

        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(0, 1));
        assertEquals('$', state.getTile(0, 2));

        game.selectLevel(1);
        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(0, 1));
        assertEquals('$', state.getTile(0, 2));
        assertEquals('$', state.getTile(0, 3));
    }

    @Test
    void boxCanBePushedUpDownAndLeft() {
        // Rightward pushes are covered above; these fixtures cover the other directions.
        State state = new State(List.of(
                List.of("#####", "#   #", "# $ #", "# @ #", "#####"),
                List.of("#####", "# @ #", "# $ #", "#   #", "#####"),
                List.of("######", "# $@ #", "######")));
        GameLogic game = new GameLogic(state);

        game.moveCharacter(-1, 0);
        assertEquals('$', state.getTile(1, 2));
        assertEquals('@', state.getTile(2, 2));

        game.selectLevel(1);
        game.moveCharacter(1, 0);
        assertEquals('$', state.getTile(3, 2));
        assertEquals('@', state.getTile(2, 2));

        game.selectLevel(2);
        game.moveCharacter(0, -1);
        assertEquals('$', state.getTile(1, 1));
        assertEquals('@', state.getTile(1, 2));
    }

    @Test
    void playerCanStartOnGoalAndLeaveIt() {
        // A starting + must behave like a player while preserving the goal below.
        State state = new State(List.of(List.of("#+ #")));
        GameLogic game = new GameLogic(state);

        game.moveCharacter(0, 1);
        assertEquals('.', state.getTile(0, 1));
        assertEquals('@', state.getTile(0, 2));
        game.moveCharacter(0, -1);
        assertEquals('+', state.getTile(0, 1));
    }

    @Test
    void missingSquaresBlockWalkingAndPushing() {
        // Include a short row, an empty row, and a box at the right-hand edge.
        State state = new State(List.of(List.of("@ ", ""), List.of("@$")));
        GameLogic game = new GameLogic(state);

        // Missing neighbors block movement, but a real floor square remains walkable.
        game.moveCharacter(-1, 0);
        game.moveCharacter(0, -1);
        game.moveCharacter(1, 0);
        assertEquals('@', state.getTile(0, 0));
        game.moveCharacter(0, 1);
        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(0, 1));

        // The box cannot be pushed beyond the end of its row.
        game.selectLevel(1);
        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(0, 0));
        assertEquals('$', state.getTile(0, 1));
    }

    @Test
    void everyLevelGetsANameAndASaveKeyFromItsOwnCollection() {
        List<String> level = List.of("#@$.#");
        State state = new State(List.of(level, level, level));
        List<LevelCollection> collections = List.of(
                new LevelCollection("/first.txt", "First", 2),
                new LevelCollection("/second.txt", "Second", 1));
        GameLogic game = new GameLogic(state, new PlayerProgress(), collections);

        // Numbers start again at 1 inside each collection.
        assertEquals("/first.txt:1", game.getLevelId(0));
        assertEquals("/first.txt:2", game.getLevelId(1));
        assertEquals("/second.txt:1", game.getLevelId(2));
        assertEquals("First 1", game.getLevelName(0));
        assertEquals("First 2", game.getLevelName(1));
        assertEquals("Second 1", game.getLevelName(2));
    }

    @Test
    void finishingALevelSavesTheRecordUnderItsOwnCollection() {
        List<String> level = List.of("#@$.#");
        State state = new State(List.of(level, level));
        PlayerProgress progress = new PlayerProgress();
        List<LevelCollection> collections = List.of(
                new LevelCollection("/first.txt", "First", 1),
                new LevelCollection("/second.txt", "Second", 1));
        GameLogic game = new GameLogic(state, progress, collections);

        game.selectLevel(1);
        game.moveCharacter(0, 1);

        assertTrue(progress.isCompleted("/second.txt:1"));
        assertFalse(progress.isCompleted("/first.txt:1"));
    }

    @Test
    void collectionsThatDoNotMatchTheLevelsAreRejected() {
        State state = new State(List.of(List.of("#@$.#"), List.of("#@$.#")));
        List<LevelCollection> tooFew = List.of(new LevelCollection("/first.txt", "First", 1));

        assertThrows(IllegalArgumentException.class,
                () -> new GameLogic(state, new PlayerProgress(), tooFew));
    }

    @Test
    void theGeneratedLevelHasNoSaveKeyOrName() {
        State state = new State(List.of(List.of("#@$.#")));
        GameLogic game = new GameLogic(state);
        game.playGeneratedLevel(List.of("#@ $.#"));

        assertThrows(IllegalArgumentException.class, () -> game.getLevelId(1));
        assertThrows(IllegalArgumentException.class, () -> game.getLevelName(1));
        assertThrows(IllegalArgumentException.class, () -> game.getLevelId(-1));
    }

    @Test
    void playingAGeneratedLevelStartsItFreshAndSelectsIt() {
        State state = new State(List.of(List.of("#@$.#")));
        GameLogic game = new GameLogic(state);

        game.playGeneratedLevel(List.of("#@ $.#"));

        assertEquals(1, state.getCurrentLevelIndex());
        assertEquals('@', state.getTile(0, 1));
        assertFalse(game.isAttemptCompleted());
    }

    @Test
    void finishingAGeneratedLevelDoesNotSaveARecord() {
        State state = new State(List.of(List.of("#@$.#")));
        GameLogic game = new GameLogic(state);
        game.playGeneratedLevel(List.of("#@$.#"));

        game.moveCharacter(0, 1);

        assertTrue(game.isAttemptCompleted());
        assertTrue(game.getProgress().getRecords().isEmpty());
    }

    @Test
    void finishingASavedLevelStillSavesARecord() {
        State state = new State(List.of(List.of("#@$.#")));
        GameLogic game = new GameLogic(state);

        game.moveCharacter(0, 1);

        assertEquals(1, game.getProgress().getRecords().size());
    }

    @Test
    void selectingLevelResetsBoardAndPlayerPosition() {
        State state = new State(List.of(List.of("#@  #"), List.of("#  @ #")));
        GameLogic game = new GameLogic(state);

        // Selecting the current level must restart it from the saved player position.
        game.moveCharacter(0, 1);
        game.selectLevel(0);
        assertEquals('@', state.getTile(0, 1));
        assertEquals(' ', state.getTile(0, 2));

        // Switching layouts must also refresh GameLogic's cached player coordinates.
        game.selectLevel(1);
        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(0, 4));

        game.selectLevel(0);
        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(0, 2));
    }

    // ------------------------------------------------------------------------------
    // Undo
    // ------------------------------------------------------------------------------

    @Test
    void undoTakesBackTheLastStepAndItsCost() {
        State state = new State(List.of(List.of("#@  #")));
        GameLogic game = new GameLogic(state);
        game.moveCharacter(0, 1);
        game.moveCharacter(0, 1);

        assertTrue(game.undoMove());

        // The player is back where they were after the first step, and only one step counts.
        assertEquals('@', state.getTile(0, 2));
        assertEquals(' ', state.getTile(0, 3));
        assertEquals(1, state.getMoveCount());
        assertEquals(State.STARTING_SCORE - State.MOVE_PENALTY, state.getScore());
    }

    @Test
    void undoPutsAPushedBoxBackAndRestoresTheGoalUnderIt() {
        State state = new State(List.of(List.of("#######", "#@$.  #", "#$ .  #", "#######")));
        GameLogic game = new GameLogic(state);
        game.moveCharacter(0, 1); // Push the box onto the goal: row 1 becomes "# @*  #".
        assertEquals('*', state.getTile(1, 3));

        game.undoMove();

        assertEquals(List.of("#######", "#@$.  #", "#$ .  #", "#######"),
                state.getBoardSnapshot());
    }

    @Test
    void undoCanStepAllTheWayBackToTheStart() {
        List<String> level = List.of("#####", "#@  #", "# $ #", "#  .#", "#####");
        State state = new State(List.of(level));
        GameLogic game = new GameLogic(state);
        game.moveCharacter(0, 1);
        game.moveCharacter(1, 0);
        game.moveCharacter(0, -1);

        // Three undos for three moves, and then there is nothing left to undo.
        assertTrue(game.undoMove());
        assertTrue(game.undoMove());
        assertTrue(game.undoMove());
        assertFalse(game.undoMove());

        assertEquals(level, state.getBoardSnapshot());
        assertEquals(0, state.getMoveCount());
    }

    @Test
    void undoWithNothingToUndoChangesNothing() {
        State state = new State(List.of(List.of("#@  #")));
        GameLogic game = new GameLogic(state);

        assertFalse(game.canUndo());
        assertFalse(game.undoMove());
        assertEquals(List.of("#@  #"), state.getBoardSnapshot());
    }

    @Test
    void blockedMovesCannotBeUndoneBecauseTheyNeverHappened() {
        State state = new State(List.of(List.of("#@  #")));
        GameLogic game = new GameLogic(state);
        game.moveCharacter(0, -1); // Straight into the wall.

        assertFalse(game.canUndo());
    }

    @Test
    void restartingOrChangingLevelForgetsTheUndoHistory() {
        State state = new State(List.of(List.of("#@  #"), List.of("#@  #")));
        GameLogic game = new GameLogic(state);

        game.moveCharacter(0, 1);
        game.selectLevel(0); // Restart the same level.
        assertFalse(game.canUndo());

        game.moveCharacter(0, 1);
        game.selectLevel(1); // Switch to another level.
        assertFalse(game.canUndo());
        game.selectLevel(0); // And back: nothing from before is remembered.
        assertFalse(game.canUndo());
    }

    @Test
    void aFinishedLevelCannotBeUndone() {
        State state = new State(List.of(List.of("#@$.#")));
        GameLogic game = new GameLogic(state);
        game.moveCharacter(0, 1); // Solves the level.
        assertTrue(game.isAttemptCompleted());

        // The result was already recorded, so the attempt stays frozen until a restart.
        assertFalse(game.canUndo());
        assertFalse(game.undoMove());
        assertTrue(game.isAttemptCompleted());
        assertEquals(1, state.getMoveCount());
    }

    @Test
    void movingAgainAfterAnUndoWorksNormally() {
        State state = new State(List.of(List.of("#@  #")));
        GameLogic game = new GameLogic(state);
        game.moveCharacter(0, 1);
        game.undoMove();

        game.moveCharacter(0, 1);
        game.moveCharacter(0, 1);

        assertEquals('@', state.getTile(0, 3));
        assertEquals(2, state.getMoveCount());
        // Both steps can still be taken back one at a time.
        assertTrue(game.undoMove());
        assertTrue(game.undoMove());
        assertFalse(game.undoMove());
    }

    // ------------------------------------------------------------------------------
    // Stuck boxes
    // ------------------------------------------------------------------------------

    @Test
    void pushingABoxIntoADeadEndIsReportedAndUndoClearsTheWarning() {
        // Pushing the box left puts it against the left wall, and no goal can be reached from
        // there.
        State state = new State(List.of(List.of(
                "######", "#  . #", "# $  #", "#  @ #", "######")));
        GameLogic game = new GameLogic(state);
        assertFalse(game.hasStuckBox());

        game.moveCharacter(-1, 0); // Step up so the player is beside the box.
        game.moveCharacter(0, -1); // Push the box left, against the wall.
        assertTrue(game.hasStuckBox());

        game.undoMove();
        assertFalse(game.hasStuckBox());
    }

    // ------------------------------------------------------------------------------
    // Moving between levels
    // ------------------------------------------------------------------------------

    @Test
    void nextAndPreviousLevelMoveOneStepAndStopAtTheEnds() {
        State state = new State(List.of(
                List.of("#@#"), List.of("# @#"), List.of("#  @#")));
        GameLogic game = new GameLogic(state);

        assertFalse(game.selectPreviousLevel()); // Already at the first level.
        assertEquals(0, state.getCurrentLevelIndex());

        assertTrue(game.selectNextLevel());
        assertTrue(game.selectNextLevel());
        assertEquals(2, state.getCurrentLevelIndex());
        assertFalse(game.selectNextLevel()); // Already at the last level.
        assertEquals(2, state.getCurrentLevelIndex());

        assertTrue(game.selectPreviousLevel());
        assertEquals(1, state.getCurrentLevelIndex());
    }

    @Test
    void nextLevelStartsTheLevelFreshAndUnfreezesTheGame() {
        State state = new State(List.of(List.of("#@$.#"), List.of("#@  #")));
        GameLogic game = new GameLogic(state);
        game.moveCharacter(0, 1);
        assertTrue(game.isAttemptCompleted());

        game.selectNextLevel();

        assertFalse(game.isAttemptCompleted());
        assertEquals(0, state.getMoveCount());
        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(0, 2));
    }

    @Test
    void nextLevelCanReachTheGeneratedLevelAfterTheLastSavedOne() {
        State state = new State(List.of(List.of("#@#"), List.of("# @#")));
        GameLogic game = new GameLogic(state);
        game.playGeneratedLevel(List.of("#  @#"));
        game.selectLevel(1);

        assertTrue(game.selectNextLevel());

        assertTrue(state.isGeneratedLevel(state.getCurrentLevelIndex()));
        assertFalse(game.selectNextLevel());
    }

    @Test
    void firstUnfinishedLevelSkipsLevelsThePlayerHasCompleted() {
        State state = new State(List.of(
                List.of("#@#"), List.of("# @#"), List.of("#  @#")));
        PlayerProgress progress = new PlayerProgress();
        GameLogic game = new GameLogic(state, progress, "set");
        progress.recordCompletion("set:1", 1000, 10);
        progress.recordCompletion("set:3", 1000, 10);

        game.selectFirstUnfinishedLevel();

        // Level 2 is the first one not finished, even though level 3 is finished too.
        assertEquals(1, state.getCurrentLevelIndex());
    }

    @Test
    void firstUnfinishedLevelStaysPutWhenEverythingIsFinished() {
        State state = new State(List.of(List.of("#@#"), List.of("# @#")));
        PlayerProgress progress = new PlayerProgress();
        GameLogic game = new GameLogic(state, progress, "set");
        progress.recordCompletion("set:1", 1000, 10);
        progress.recordCompletion("set:2", 1000, 10);

        game.selectFirstUnfinishedLevel();

        assertEquals(0, state.getCurrentLevelIndex());
    }

    @Test
    void firstUnfinishedLevelIsTheFirstLevelForANewPlayer() {
        State state = new State(List.of(List.of("#@#"), List.of("# @#")));
        GameLogic game = new GameLogic(state);

        game.selectFirstUnfinishedLevel();

        assertEquals(0, state.getCurrentLevelIndex());
    }
}
