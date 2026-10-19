package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Check the level files that ship with the game, since a typo in one would break startup. */
class BundledLevelsTest {
    @Test
    void bothCollectionsLoadWithTheCountsTheReadmePromises() throws IOException {
        LevelLoader loader = new LevelLoader();

        assertEquals(51, loader.loadResource("/original-levels.txt").size());
        assertEquals(155, loader.loadResource("/microban.txt").size());
    }

    @Test
    void everyBundledLevelHasExactlyOnePlayerAndAtLeastOneBox() throws IOException {
        LevelLoader loader = new LevelLoader();
        List<List<String>> all = new ArrayList<>(loader.loadResource("/original-levels.txt"));
        all.addAll(loader.loadResource("/microban.txt"));

        for (int index = 0; index < all.size(); index++) {
            List<String> rows = all.get(index);
            int players = 0;
            int boxes = 0;
            for (String row : rows) {
                for (int column = 0; column < row.length(); column++) {
                    if (Tiles.isPlayer(row.charAt(column))) {
                        players++;
                    }
                    if (Tiles.isBox(row.charAt(column))) {
                        boxes++;
                    }
                }
            }
            assertEquals(1, players, "Level " + (index + 1) + " needs exactly one player.");
            assertTrue(boxes > 0, "Level " + (index + 1) + " has no boxes.");
        }
    }

    @Test
    void theGameCanHoldBothCollectionsTogether() throws IOException {
        LevelLoader loader = new LevelLoader();
        List<List<String>> original = loader.loadResource("/original-levels.txt");
        List<List<String>> microban = loader.loadResource("/microban.txt");
        List<List<String>> all = new ArrayList<>(original);
        all.addAll(microban);

        State state = new State(all);
        List<LevelCollection> collections = List.of(
                new LevelCollection("/original-levels.txt", "Original", original.size()),
                new LevelCollection("/microban.txt", "Microban", microban.size()));
        GameLogic game = new GameLogic(state, new PlayerProgress(), collections);

        assertEquals(206, state.getLevelCount());
        assertEquals("/original-levels.txt:51", game.getLevelId(50));
        assertEquals("/microban.txt:1", game.getLevelId(51));
        assertEquals("Microban 155", game.getLevelName(205));
    }
}
