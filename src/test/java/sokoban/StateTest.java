package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verify shared state, defensive copies, and validation independently of the UI. */
class StateTest {
    @Test
    void gameActionsUpdateTheSharedState() {
        // Use distinct templates to make a successful level change observable.
        List<String> first = List.of("###", "#@#", "###");
        List<String> second = List.of("#####", "# @ #", "#####");
        State state = new State(List.of(first, second));
        GameLogic game = new GameLogic(state);

        // A game action must update the original State instance supplied to GameLogic.
        assertEquals(0, state.getCurrentLevelIndex());
        assertEquals(first, state.getCurrentLevel());
        game.selectLevel(1);
        assertEquals(1, state.getCurrentLevelIndex());
        assertEquals(second, state.getCurrentLevel());
    }

    @Test
    void rejectedSelectionLeavesTheCurrentLevelUnchanged() {
        State state = new State(List.of(List.of("#@#"), List.of("# @#")));
        GameLogic game = new GameLogic(state);
        game.selectLevel(1);

        // Both ends of the valid index range are guarded before state is changed.
        assertThrows(IllegalArgumentException.class, () -> game.selectLevel(-1));
        assertThrows(IllegalArgumentException.class, () -> game.selectLevel(2));
        assertEquals(1, state.getCurrentLevelIndex());
    }

    @Test
    void savedLayoutsCannotBeChangedThroughOutsideLists() {
        // Mutable input lists reveal whether State accidentally keeps caller-owned data.
        List<String> rows = new ArrayList<>(List.of("#@#"));
        List<List<String>> levels = new ArrayList<>();
        levels.add(rows);
        State state = new State(levels);

        // Alter both nesting levels after construction; the saved template must survive.
        rows.set(0, "changed");
        levels.clear();

        assertEquals(List.of("#@#"), state.getCurrentLevel());
        // The getter must not offer a way to mutate the preserved template either.
        assertThrows(UnsupportedOperationException.class,
                () -> state.getCurrentLevel().set(0, "changed"));
    }

    @Test
    void generatedLevelIsListedAfterTheSavedLevelsAndSelected() {
        State state = new State(List.of(List.of("#@#"), List.of("# @#")));
        assertEquals(2, state.getLevelCount());
        assertFalse(state.isGeneratedLevel(1));

        state.setGeneratedLevel(List.of("#@ #"));

        // There is now a third level, which is selected and starts as a fresh board.
        assertEquals(3, state.getLevelCount());
        assertEquals(2, state.getSavedLevelCount());
        assertEquals(2, state.getCurrentLevelIndex());
        assertTrue(state.isGeneratedLevel(2));
        assertEquals(List.of("#@ #"), state.getCurrentLevel());
        assertEquals(0, state.getMoveCount());
    }

    @Test
    void makingAnotherGeneratedLevelReplacesTheFirst() {
        State state = new State(List.of(List.of("#@#")));
        state.setGeneratedLevel(List.of("#@ #"));
        state.setGeneratedLevel(List.of("#  @#"));

        assertEquals(2, state.getLevelCount());
        assertEquals(List.of("#  @#"), state.getCurrentLevel());
    }

    @Test
    void savedLevelsCanStillBeSelectedAfterAGeneratedLevelExists() {
        State state = new State(List.of(List.of("#@#"), List.of("# @#")));
        state.setGeneratedLevel(List.of("#@ #"));

        state.setCurrentLevelIndex(1);

        assertEquals(List.of("# @#"), state.getCurrentLevel());
        assertFalse(state.isGeneratedLevel(state.getCurrentLevelIndex()));
        // The generated level is still there to go back to.
        state.setCurrentLevelIndex(2);
        assertEquals(List.of("#@ #"), state.getCurrentLevel());
    }

    @Test
    void generatedLevelMustContainABoard() {
        State state = new State(List.of(List.of("#@#")));

        assertThrows(IllegalArgumentException.class, () -> state.setGeneratedLevel(List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> state.setGeneratedLevel(List.of("   ")));
        // A rejected level changes nothing.
        assertEquals(1, state.getLevelCount());
    }

    @Test
    void movesAreCountedAndUndoRestoresTheBoardBeforeThem() {
        State state = new State(List.of(List.of("#@  #")));
        assertFalse(state.canUndo());

        state.applyMove(List.of("# @ #"));
        state.applyMove(List.of("#  @#"));
        assertEquals(2, state.getMoveCount());
        assertTrue(state.canUndo());

        // Each undo goes back one board, newest first, and takes the move count with it.
        assertTrue(state.undoMove());
        assertEquals(List.of("# @ #"), state.getBoardSnapshot());
        assertEquals(1, state.getMoveCount());
        assertTrue(state.undoMove());
        assertEquals(List.of("#@  #"), state.getBoardSnapshot());
        assertEquals(0, state.getMoveCount());
        assertFalse(state.undoMove());
        assertFalse(state.canUndo());
    }

    @Test
    void scoreGoesBackUpWhenAMoveIsUndone() {
        State state = new State(List.of(List.of("#@  #")));
        state.applyMove(List.of("# @ #"));
        assertEquals(State.STARTING_SCORE - State.MOVE_PENALTY, state.getScore());

        state.undoMove();

        assertEquals(State.STARTING_SCORE, state.getScore());
    }

    @Test
    void selectingALevelForgetsTheMovesMadeOnTheOldBoard() {
        State state = new State(List.of(List.of("#@  #"), List.of("#@  #")));
        state.applyMove(List.of("# @ #"));

        state.setCurrentLevelIndex(1);

        assertFalse(state.canUndo());
        assertEquals(0, state.getMoveCount());
    }

    @Test
    void rejectsMissingBoardsBeforeTheRendererUsesThem() {
        // Reject no levels, an empty level, and a level consisting only of whitespace.
        assertThrows(IllegalArgumentException.class, () -> new State(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new State(List.of(List.of())));
        assertThrows(IllegalArgumentException.class,
                () -> new State(List.of(List.of("", "   "))));
    }
}
