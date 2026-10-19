package sokoban;

/**
 * A player's personal bests for one completed level.
 * A score of zero still counts: the level was finished.
 *
 * @param bestScore   the highest score reached on any completion
 * @param fewestMoves the lowest move count reached on any completion
 */
public record PlayerRecord(int bestScore, int fewestMoves) {
    /** Reject impossible values, which also catches a damaged save file. */
    public PlayerRecord {
        if (bestScore < 0 || bestScore > State.STARTING_SCORE || fewestMoves < 0) {
            throw new IllegalArgumentException("Invalid player record: score " + bestScore
                    + ", moves " + fewestMoves);
        }
    }
}
