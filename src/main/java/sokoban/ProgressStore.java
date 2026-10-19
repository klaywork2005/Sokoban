package sokoban;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Properties;

/**
 * Loads and saves one local player's records as a UTF-8 properties file.
 *
 * <p>
 * Each completed level is stored as three lines, for example:
 * 
 * <pre>
 * /original-levels.txt:3.completed=true
 * /original-levels.txt:3.bestScore=9500
 * /original-levels.txt:3.fewestMoves=40
 * </pre>
 */
public final class ProgressStore {
  // The three key endings that describe one level; the level ID comes before
  // each.
  private static final String COMPLETED_SUFFIX = ".completed";
  private static final String BEST_SCORE_SUFFIX = ".bestScore";
  private static final String FEWEST_MOVES_SUFFIX = ".fewestMoves";

  private final Path path;

  /** Use the given file for loading and saving. It does not need to exist yet. */
  public ProgressStore(Path path) {
    this.path = path.toAbsolutePath();
  }

  /**
   * The usual save location, in the user's home folder and outside the project.
   */
  public static Path defaultPath() {
    return Path.of(System.getProperty("user.home"), ".sokoban", "progress.properties");
  }

  /**
   * Read the saved records. A missing file is normal on the first launch and
   * gives
   * empty progress.
   *
   * @throws IOException if the file cannot be read or its contents are invalid.
   *                     A damaged file is reported rather than silently replaced.
   */
  public PlayerProgress load() throws IOException {
    PlayerProgress progress = new PlayerProgress();
    if (Files.notExists(path)) {
      return progress;
    }

    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      Properties properties = new Properties();
      properties.load(reader);
      for (String key : properties.stringPropertyNames()) {
        if (isCompletedKey(key, properties)) {
          String levelId = key.substring(0, key.length() - COMPLETED_SUFFIX.length());
          readRecord(properties, levelId, progress);
        }
      }
    } catch (IllegalArgumentException exception) {
      // Number-format and record-validation errors both end up here.
      throw new IOException("Invalid progress data in " + path, exception);
    }
    return progress;
  }

  /** True for a line such as {@code someLevel.completed=true}. */
  private static boolean isCompletedKey(String key, Properties properties) {
    return key.endsWith(COMPLETED_SUFFIX) && "true".equals(properties.getProperty(key));
  }

  /**
   * Copy one level's score and move count from the file into the progress object.
   */
  private static void readRecord(Properties properties, String levelId, PlayerProgress progress) {
    int score = readInt(properties, levelId + BEST_SCORE_SUFFIX);
    int moves = readInt(properties, levelId + FEWEST_MOVES_SUFFIX);
    progress.recordCompletion(levelId, score, moves);
  }

  private static int readInt(Properties properties, String key) {
    String text = properties.getProperty(key);
    if (text == null) {
      throw new IllegalArgumentException("Missing value for " + key);
    }
    return Integer.parseInt(text);
  }

  /**
   * Write all records to the save file.
   *
   * <p>
   * The data goes to a temporary file first and then replaces the old save, so a
   * crash
   * halfway through writing cannot leave a half-written save behind.
   *
   * @throws IOException if the file cannot be written
   */
  public void save(PlayerProgress progress) throws IOException {
    Properties properties = toProperties(progress);

    Files.createDirectories(path.getParent());
    Path temporary = Files.createTempFile(path.getParent(), "progress-", ".tmp");
    try {
      try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
        properties.store(writer, "Sokoban completed levels and personal bests");
      }
      replaceSaveFile(temporary);
    } finally {
      // After a successful move the temporary file is gone; after a failure this
      // cleans up.
      Files.deleteIfExists(temporary);
    }
  }

  private static Properties toProperties(PlayerProgress progress) {
    Properties properties = new Properties();
    for (Map.Entry<String, PlayerRecord> entry : progress.getRecords().entrySet()) {
      String levelId = entry.getKey();
      PlayerRecord record = entry.getValue();
      properties.setProperty(levelId + COMPLETED_SUFFIX, "true");
      properties.setProperty(levelId + BEST_SCORE_SUFFIX,
          Integer.toString(record.bestScore()));
      properties.setProperty(levelId + FEWEST_MOVES_SUFFIX,
          Integer.toString(record.fewestMoves()));
    }
    return properties;
  }

  /**
   * Move the finished temporary file over the real save, atomically when the
   * system allows.
   */
  private void replaceSaveFile(Path temporary) throws IOException {
    try {
      Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException exception) {
      Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
