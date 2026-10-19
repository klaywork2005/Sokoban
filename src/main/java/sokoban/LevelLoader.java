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

/**
 * Reads levels from XSB text files. It does not choose levels or change the
 * game state.
 *
 * <p>
 * In an XSB file, consecutive lines made only of tile symbols form one level.
 * Anything else, such as a blank line or a title, ends the level being read.
 */
public final class LevelLoader {
  /**
   * Load a level file bundled with the program, such as
   * {@code "/original-levels.txt"}.
   * Maven copies {@code src/main/resources} onto the classpath, so this also
   * works from a
   * JAR and does not depend on which folder the program was started from.
   *
   * @throws IOException if the resource does not exist or cannot be read
   */
  public List<List<String>> loadResource(String resourceName) throws IOException {
    // A leading slash makes the name start at the classpath root instead of this
    // package.
    InputStream stream = LevelLoader.class.getResourceAsStream(resourceName);
    if (stream == null) {
      throw new IOException("Level resource not found: " + resourceName);
    }
    // try-with-resources closes the reader and its stream even if reading fails.
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      return readLevels(reader);
    }
  }

  /**
   * Load levels from a file on disk. A relative path starts in the program's
   * working folder.
   *
   * @throws IOException if the file cannot be read
   */
  public List<List<String>> loadLevels(Path path) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return readLevels(reader);
    }
  }

  /** Collect runs of board rows into levels. */
  private List<List<String>> readLevels(BufferedReader reader) throws IOException {
    List<List<String>> levels = new ArrayList<>();
    List<String> currentLevel = new ArrayList<>(); // Rows of the level being read.

    String line = reader.readLine();
    while (line != null) {
      // Rows are never trimmed: leading and trailing spaces position the tiles.
      if (isBoardRow(line)) {
        currentLevel.add(line);
      } else if (!currentLevel.isEmpty()) {
        // A non-board line (blank, title, comment) ends the level in progress.
        levels.add(List.copyOf(currentLevel)); // Copy before clearing the working list.
        currentLevel.clear();
      }
      line = reader.readLine();
    }

    // The last level has no blank line after it, so add it here.
    if (!currentLevel.isEmpty()) {
      levels.add(List.copyOf(currentLevel));
    }
    return List.copyOf(levels); // Read-only: callers cannot edit the loaded levels.
  }

  /** A board row is not blank and contains only valid tile symbols. */
  private static boolean isBoardRow(String line) {
    // A row of only spaces is a separator, even though a space is also a floor
    // tile.
    if (line.isBlank()) {
      return false;
    }
    for (int column = 0; column < line.length(); column++) {
      if (!Tiles.isValid(line.charAt(column))) {
        return false;
      }
    }
    return true;
  }
}
