package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Check that a level collection refuses obviously wrong values. */
class LevelCollectionTest {
    @Test
    void keepsTheValuesItWasGiven() {
        LevelCollection collection = new LevelCollection("/microban.txt", "Microban", 155);

        assertEquals("/microban.txt", collection.id());
        assertEquals("Microban", collection.name());
        assertEquals(155, collection.size());
    }

    @Test
    void rejectsBlankNamesAndNegativeSizes() {
        assertThrows(IllegalArgumentException.class, () -> new LevelCollection("", "Name", 1));
        assertThrows(IllegalArgumentException.class, () -> new LevelCollection(" ", "Name", 1));
        assertThrows(IllegalArgumentException.class, () -> new LevelCollection("/a.txt", "", 1));
        assertThrows(IllegalArgumentException.class, () -> new LevelCollection(null, "Name", 1));
        assertThrows(IllegalArgumentException.class, () -> new LevelCollection("/a.txt", null, 1));
        assertThrows(IllegalArgumentException.class, () -> new LevelCollection("/a.txt", "A", -1));
    }
}
