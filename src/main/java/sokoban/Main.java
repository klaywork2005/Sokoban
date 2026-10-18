package sokoban;

import java.io.IOException;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Entry point: load data, connect the objects, and open the window. */
public final class Main {
    // Absolute classpath path to the collection used when the application starts.
    private static final String LEVEL_RESOURCE = "/original-levels.txt";

    private Main() {
        // No Main object is needed: Java calls the static main method directly.
    }

    /** Assemble one shared game model and schedule the graphical interface. */
    public static void main(String[] args) {
        try {
            // Read files before starting the UI so disk access does not freeze it.
            LevelLoader loader = new LevelLoader();
            List<List<String>> levels = loader.loadResource(LEVEL_RESOURCE);
            // State owns the data; GameLogic applies movement rules to that same data.
            State state = new State(levels);
            GameLogic game = new GameLogic(state);

            // Swing components must be created and updated on its event thread.
            // The code inside () -> { ... } is a task Swing will run on that thread.
            SwingUtilities.invokeLater(() -> {
                GameWindow window = new GameWindow(state, game);
                window.setVisible(true);
            });
        } catch (IOException | IllegalArgumentException exception) {
            // Report missing files or unusable level data to terminal and GUI users.
            String message = "Could not start Sokoban: " + exception.getMessage();
            System.err.println(message);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null, message, "Sokoban startup error", JOptionPane.ERROR_MESSAGE));
        }
    }
}
