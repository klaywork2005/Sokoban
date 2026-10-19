package sokoban;

/**
 * One group of levels that came from the same level file, such as the Original levels.
 *
 * <p>The game keeps all of its levels together in one list. This record remembers how many
 * levels each file contributed, so that every level can be given a name the player sees
 * ("Microban 12") and a save key that stays the same when other collections are added.
 *
 * @param id   names the level file, for example {@code "/microban.txt"}. It is part of the
 *             save key of every level in the collection, so it must not change.
 * @param name what the player sees, for example {@code "Microban"}
 * @param size how many levels the file contained
 */
public record LevelCollection(String id, String name, int size) {
    /**
     * @throws IllegalArgumentException if the id or name is blank, or the size is negative
     */
    public LevelCollection {
        if (id == null || id.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("A collection needs an id and a name.");
        }
        if (size < 0) {
            throw new IllegalArgumentException("A collection cannot have a negative size.");
        }
    }
}
