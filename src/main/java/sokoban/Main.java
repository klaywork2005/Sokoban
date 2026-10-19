package sokoban;

import java.io.IOException;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Program entry point: loads the data, connects the objects, and opens the window. */
public final class Main {
    // Classpath location of the level collection shown at startup.
    // Use "/microban.txt" to play the Microban collection instead.
    private static final String LEVEL_RESOURCE = "/original-levels.txt";

    private Main() {
        // Java calls the static main method directly, so no Main object is ever needed.
    }

    /** Build the one shared game model, then open the window on Swing's event thread. */
    public static void main(String[] args) {
        try {
            // Read files before the UI starts so slow disk access cannot freeze the window.
            LevelLoader loader = new LevelLoader();
            List<List<String>> levels = loader.loadResource(LEVEL_RESOURCE);
            State state = new State(levels);
            ProgressStore progressStore = new ProgressStore(ProgressStore.defaultPath());
            PlayerProgress progress = progressStore.load();
            // The resource name keeps records for different collections apart in one save file.
            GameLogic game = new GameLogic(state, progress, LEVEL_RESOURCE);

            // Swing components must be created on the event thread. The code in
            // () -> { ... } is a task that Swing runs there for us.
            SwingUtilities.invokeLater(() -> {
                GameWindow window = new GameWindow(state, game, progressStore);
                window.setVisible(true);
                // The music plays on its own thread, so it never slows the window down.
                new BackgroundMusic().start();
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
