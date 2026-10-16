package sokoban;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LevelLoader {

  /**
   * Reads a level collection from a file. Relative paths start at the program's
   * working directory, not the directory containing this Java class.
   */
  public List<List<String>> loadLevels(Path path) throws IOException {
    // Read each file line into a string, preserving spaces used to position tiles.
    List<String> lines = Files.readAllLines(path);

    // The outer list holds complete levels; each inner list holds one level's rows.
    List<List<String>> levels = new ArrayList<>();
    // Collect rows here until a separator marks the end of the current board.
    List<String> currentLevel = new ArrayList<>();

    for (String line : lines) {
        // A board row must be nonblank and contain only Sokoban tile characters:
        // # wall, space floor, . goal, $ box, @ player,
        // * box on a goal, + player on a goal.
        // The final + outside the brackets means "one or more characters".
        boolean isBoardRow = !line.isBlank() && line.matches("[ #.$@*+]+");

        if (isBoardRow) {
          // Keep the original row: trimming it would shift tiles to the left.
          currentLevel.add(line);
        } else if (!currentLevel.isEmpty()) {
          // A blank line, comment, or metadata line ends a board we are collecting.
          levels.add(currentLevel);
          // Use a new list so collecting the next board does not change the saved one.
          currentLevel = new ArrayList<>();
        }

    }

    // Save the last level if the file ends without a separator.
    if (!currentLevel.isEmpty()) {
      levels.add(currentLevel);
    }
    
    return levels;
  }
  
}
