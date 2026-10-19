package sokoban;

import java.util.HashMap;
import java.util.Map;

/**
 * The player's records for every level they have completed, kept in memory.
 * This class does not know about the current attempt or about files;
 * {@link ProgressStore} handles saving and loading.
 */
public final class PlayerProgress {
    // Maps a level ID such as "/original-levels.txt:3" to the best results on that level.
    private final Map<String, PlayerRecord> records = new HashMap<>();

    /** The record for a level, or null if the player has not completed it yet. */
    public PlayerRecord getRecord(String levelId) {
        return records.get(levelId);
    }

    /** True if the player has finished this level at least once. */
    public boolean isCompleted(String levelId) {
        return records.containsKey(levelId);
    }

    /**
     * Record a finished attempt. Replaying a level can improve its record
     * but can never erase an earlier best.
     *
     * @throws IllegalArgumentException if the score or move count is impossible
     */
    public void recordCompletion(String levelId, int score, int moves) {
        // Creating the record first rejects impossible values before anything is stored.
        PlayerRecord attempt = new PlayerRecord(score, moves);

        PlayerRecord previous = records.get(levelId);
        if (previous == null) {
            records.put(levelId, attempt);
            return;
        }

        // Keep the higher score and the lower move count, even if they come from different runs.
        int bestScore = Math.max(previous.bestScore(), attempt.bestScore());
        int fewestMoves = Math.min(previous.fewestMoves(), attempt.fewestMoves());
        records.put(levelId, new PlayerRecord(bestScore, fewestMoves));
    }

    /** A read-only copy, so a caller such as the save code cannot change the live records. */
    public Map<String, PlayerRecord> getRecords() {
        return Map.copyOf(records);
    }
}
