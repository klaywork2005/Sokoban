package sokoban;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads XSB boards; it does not select levels or change the game state. */
public final class LevelLoader {
    /**
     * Load a bundled resource, such as "/original-levels.txt".
     * Maven copies src/main/resources to the classpath, so this also works in a JAR
     * and does not depend on which directory the program was launched from.
     */
    public List<List<String>> loadResource(String resourceName) throws IOException {
        // A leading slash resolves from the classpath root rather than this package.
        InputStream stream = LevelLoader.class.getResourceAsStream(resourceName);
        if (stream == null) {
            throw new IOException("Level resource not found: " + resourceName);
        }
        // try-with-resources closes the reader and its stream even if reading fails.
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return readLevels(reader);
        }
    }

    /** Load an external file. Relative paths use the program's working directory. */
    public List<List<String>> loadLevels(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return readLevels(reader);
        }
    }

    /** Collect consecutive board rows; any non-board line ends the current board. */
    private List<List<String>> readLevels(BufferedReader reader) throws IOException {
        // Keep completed boards separate from the rows of the board being read.
        List<List<String>> levels = new ArrayList<>();
        List<String> currentLevel = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            // # wall, space floor, . goal, $ box, @ player,
            // * box on goal, + player on goal. Never trim: spaces position tiles.
            if (isBoardRow(line)) {
                currentLevel.add(line);
            } else if (!currentLevel.isEmpty()) {
                // Blank lines and metadata separate boards in the bundled files.
                // Snapshot the rows before clearing the reusable accumulation list.
                levels.add(List.copyOf(currentLevel));
                currentLevel.clear();
            }
        }
        // The last board may have no blank line after it.
        if (!currentLevel.isEmpty()) {
            levels.add(List.copyOf(currentLevel));
        }
        // Neither the collection nor its saved row lists should be edited by callers.
        return List.copyOf(levels);
    }

    /** A board row contains only tile characters and is not a blank separator. */
    private boolean isBoardRow(String line) {
        if (line.isBlank()) {
            // A row of only spaces is a separator, even though space is a tile symbol.
            return false;
        }

        String allowedTiles = " #.$@*+";
        for (int column = 0; column < line.length(); column++) {
            char tile = line.charAt(column);
            // indexOf returns -1 when the character is not in allowedTiles.
            if (allowedTiles.indexOf(tile) == -1) {
                return false;
            }
        }
        return true;
    }
}
