package sokoban;

import java.util.List;
import java.util.Optional;

/**
 * Applies the player's actions to the shared {@link State}: moving, pushing boxes,
 * choosing a level, and recording a result when a level is finished. No Swing code.
 */
public final class GameLogic {
    // Used by the one-argument constructor, which is handy for tests.
    private static final String DEFAULT_COLLECTION_ID = "default";

    // This is the same state the window and renderer use, not a separate copy of the game.
    private final State state;
    private final PlayerProgress progress;
    // Part of every saved level ID, so different level collections keep separate records.
    private final String collectionId;
    // Set when the last box lands on a goal. Stays set until the level is restarted,
    // so one attempt can only ever produce one result.
    private boolean attemptCompleted;

    /** Attach the rules to a state, with empty records that are never saved. */
    public GameLogic(State state) {
        this(state, new PlayerProgress(), DEFAULT_COLLECTION_ID);
    }

    /**
     * @param state        the shared game data
     * @param progress     the player's saved records, updated when a level is completed
     * @param collectionId names the level file, e.g. {@code "/original-levels.txt"}
     */
    public GameLogic(State state, PlayerProgress progress, String collectionId) {
        this.state = state;
        this.progress = progress;
        this.collectionId = collectionId;
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

    /** The key for a level in the save file, such as {@code "/original-levels.txt:3"}. */
    public String getLevelId(int index) {
        // Level numbers in save keys start at one, just like the level picker.
        return collectionId + ":" + (index + 1);
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
        state.applyBoard(newBoard.get());
        state.recordMove();

        // The move count is updated first so the final push is included in the result.
        if (state.isSolved()) {
            finishAttempt();
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
