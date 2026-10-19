package sokoban;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Program entry point: loads the data, connects the objects, and opens the window. */
public final class Main {
    // Classpath locations of the level files. The levels of both are played together,
    // Original first. These names are part of every save key, so do not change them.
    private static final String ORIGINAL_RESOURCE = "/original-levels.txt";
    private static final String MICROBAN_RESOURCE = "/microban.txt";

    private Main() {
        // Java calls the static main method directly, so no Main object is ever needed.
    }

    /** Build the one shared game model, then open the window on Swing's event thread. */
    public static void main(String[] args) {
        try {
            // Read files before the UI starts so slow disk access cannot freeze the window.
            LevelLoader loader = new LevelLoader();
            List<List<String>> originalLevels = loader.loadResource(ORIGINAL_RESOURCE);
            List<List<String>> microbanLevels = loader.loadResource(MICROBAN_RESOURCE);

            // Put every level in one list, with the Original levels first.
            List<List<String>> levels = new ArrayList<>(originalLevels);
            levels.addAll(microbanLevels);
            State state = new State(levels);

            // The collections tell the game which levels came from which file. The file name
            // keeps the records of different collections apart in one save file.
            List<LevelCollection> collections = List.of(
                    new LevelCollection(ORIGINAL_RESOURCE, "Original", originalLevels.size()),
                    new LevelCollection(MICROBAN_RESOURCE, "Microban", microbanLevels.size()));

            ProgressStore progressStore = new ProgressStore(ProgressStore.defaultPath());
            PlayerProgress progress = progressStore.load();
            GameLogic game = new GameLogic(state, progress, collections);
            // Carry on from the first level the player has not completed yet.
            game.selectFirstUnfinishedLevel();

            // Swing components must be created on the event thread. The code in
            // () -> { ... } is a task that Swing runs there for us.
            SwingUtilities.invokeLater(() -> {
                BackgroundMusic music = new BackgroundMusic();
                GameWindow window = new GameWindow(state, game, progressStore, music);
                window.setVisible(true);
                // The music plays on its own thread, so it never slows the window down.
                music.start();
            });
        } catch (IOException | IllegalArgumentException exception) {
            showStartupError(exception);
        }
    }

    /** Report missing files or unusable level data in both the terminal and a dialog box. */
    private static void showStartupError(Exception exception) {
        String message = "Could not start Sokoban: " + exception.getMessage();
        System.err.println(message);
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                null, message, "Sokoban startup error", JOptionPane.ERROR_MESSAGE));
    }
}
