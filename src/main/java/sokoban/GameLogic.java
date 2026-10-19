package sokoban;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Applies the player's actions to the shared {@link State}: moving, pushing boxes, undoing a
 * move, choosing a level, and recording a result when a level is finished. No Swing code.
 */
public final class GameLogic {
    // Used by the one-argument constructor, which is handy for tests.
    private static final String DEFAULT_COLLECTION_ID = "default";

    // This is the same state the window and renderer use, not a separate copy of the game.
    private final State state;
    private final PlayerProgress progress;
    // One save key and one display name for every saved level, in the same order as the levels.
    // The keys include the level file, so different collections keep separate records.
    private final List<String> levelIds = new ArrayList<>();
    private final List<String> levelNames = new ArrayList<>();
    // Set when the last box lands on a goal. Stays set until the level is restarted,
    // so one attempt can only ever produce one result.
    private boolean attemptCompleted;

    /** Attach the rules to a state, with empty records that are never saved. */
    public GameLogic(State state) {
        this(state, new PlayerProgress(), DEFAULT_COLLECTION_ID);
    }

    /**
     * For a game whose saved levels all came from one level file.
     *
     * @param state        the shared game data
     * @param progress     the player's saved records, updated when a level is completed
     * @param collectionId names the level file, e.g. {@code "/original-levels.txt"}
     */
    public GameLogic(State state, PlayerProgress progress, String collectionId) {
        this(state, progress,
                List.of(new LevelCollection(collectionId, "Level", state.getSavedLevelCount())));
    }

    /**
     * @param state       the shared game data
     * @param progress    the player's saved records, updated when a level is completed
     * @param collections the level files the saved levels came from, in the order the levels
     *                    were added to the state
     * @throws IllegalArgumentException if the collections do not add up to the state's levels
     */
    public GameLogic(State state, PlayerProgress progress, List<LevelCollection> collections) {
        this.state = state;
        this.progress = progress;

        // Work out the key and name of every level, one collection after another.
        for (LevelCollection collection : collections) {
            for (int number = 1; number <= collection.size(); number++) {
                levelIds.add(collection.id() + ":" + number);
                levelNames.add(collection.name() + " " + number);
            }
        }
        if (levelIds.size() != state.getSavedLevelCount()) {
            throw new IllegalArgumentException(
                    "The collections have " + levelIds.size() + " levels but the game has "
                            + state.getSavedLevelCount() + ".");
        }
    }

    /** Load a fresh board and start a new attempt, even if this level is already loaded. */
    public void selectLevel(int index) {
        state.setCurrentLevelIndex(index);
        attemptCompleted = false;
    }

    /**
     * Start playing a freshly generated level, replacing the previous generated one.
     * Generated levels are not part of the level file, so they never get a saved record.
     */
    public void playGeneratedLevel(List<String> rows) {
        state.setGeneratedLevel(rows);
        attemptCompleted = false;
    }

    /** True once every box is on a goal. Further moves are ignored until a restart. */
    public boolean isAttemptCompleted() {
        return attemptCompleted;
    }

    /** The player's personal records. */
    public PlayerProgress getProgress() {
        return progress;
    }

    /**
     * The key for a saved level in the save file, such as {@code "/original-levels.txt:3"}.
     * The number counts from 1 within the level's own file.
     *
     * @throws IllegalArgumentException if the index is not one of the saved levels
     *                                  (the generated level has no key)
     */
    public String getLevelId(int index) {
        checkSavedLevel(index);
        return levelIds.get(index);
    }

    /**
     * The name shown to the player for a saved level, such as {@code "Microban 12"}.
     *
     * @throws IllegalArgumentException if the index is not one of the saved levels
     */
    public String getLevelName(int index) {
        checkSavedLevel(index);
        return levelNames.get(index);
    }

    private void checkSavedLevel(int index) {
        if (index < 0 || index >= levelIds.size()) {
            throw new IllegalArgumentException("Not a saved level: " + index);
        }
    }

    /**
     * Try one step by the player. Blocked moves do nothing and cost nothing.
     *
     * @param dr rows to move: -1 is up, 1 is down
     * @param dc columns to move: -1 is left, 1 is right
     */
    public void moveCharacter(int dr, int dc) {
        // A finished attempt stays frozen until the player selects or resets a level.
        if (attemptCompleted) {
            return;
        }

        Optional<List<String>> newBoard = BoardRules.tryMove(state.getBoardSnapshot(), dr, dc);
        if (newBoard.isEmpty()) {
            return; // Blocked by a wall or a box that cannot be pushed.
        }

        // Only a legal move reaches the live board and costs a penalty.
        state.applyMove(newBoard.get());

        // The move count is updated first so the final push is included in the result.
        if (state.isSolved()) {
            finishAttempt();
        }
    }

    /** True if there is a move to take back. A finished attempt cannot be changed. */
    public boolean canUndo() {
        return !attemptCompleted && state.canUndo();
    }

    /**
     * Take back the last move. The move count and score go back to what they were before it,
     * so the result of a finished level only counts the moves that were left on the board.
     * Once a level is finished it is frozen, so it cannot be undone (reset it instead).
     *
     * @return true if a move was undone
     */
    public boolean undoMove() {
        if (!canUndo()) {
            return false;
        }
        return state.undoMove();
    }

    /** True if some box can never reach a goal again, so this attempt cannot be won. */
    public boolean hasStuckBox() {
        return DeadlockDetector.hasStuckBox(state.getBoardSnapshot());
    }

    /** Start the next level in the list, if there is one. Returns false at the last level. */
    public boolean selectNextLevel() {
        int next = state.getCurrentLevelIndex() + 1;
        if (next >= state.getLevelCount()) {
            return false;
        }
        selectLevel(next);
        return true;
    }

    /** Start the level before this one, if there is one. Returns false at the first level. */
    public boolean selectPreviousLevel() {
        int previous = state.getCurrentLevelIndex() - 1;
        if (previous < 0) {
            return false;
        }
        selectLevel(previous);
        return true;
    }

    /**
     * Pick up where the player left off: select the first saved level they have not
     * completed yet. If they have completed every level, the selection stays as it is.
     */
    public void selectFirstUnfinishedLevel() {
        for (int index = 0; index < levelIds.size(); index++) {
            if (!progress.isCompleted(levelIds.get(index))) {
                selectLevel(index);
                return;
            }
        }
    }

    /** Freeze the board and save the result as a personal record (except for a generated level). */
    private void finishAttempt() {
        attemptCompleted = true;
        int levelIndex = state.getCurrentLevelIndex();
        if (state.isGeneratedLevel(levelIndex)) {
            return; // A generated level has no fixed number, so there is nothing to record against.
        }
        progress.recordCompletion(getLevelId(levelIndex), state.getScore(), state.getMoveCount());
    }
}
