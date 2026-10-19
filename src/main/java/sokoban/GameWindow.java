package sokoban;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

/**
 * The game window. It builds the controls, turns keyboard and button input into game
 * actions, and refreshes the display afterwards. The rules themselves live in
 * {@link GameLogic}, and the board is drawn by {@link LevelRenderer}.
 */
public final class GameWindow extends JFrame {
    // Swing components are Serializable, so the compiler expects a version number.
    // This window is never actually saved to disk.
    private static final long serialVersionUID = 1L;

    private static final int MIN_WIDTH = 600;
    private static final int MIN_HEIGHT = 450;

    // The window shares the model and rules; it only coordinates input and display.
    private final State state;
    private final GameLogic game;
    private final LevelRenderer renderer;
    private final ProgressStore progressStore;
    // A new Random() gives different levels every time the program runs.
    private final LevelGenerator generator = new LevelGenerator(new Random());

    // The dropdown can hold a pending choice until "Go To Level" is clicked.
    private final JComboBox<String> levelList = new JComboBox<>();
    private final JLabel status = new JLabel();

    /**
     * Build the window. {@code Main} calls this on Swing's event thread.
     *
     * @param progressStore where completed levels are saved
     */
    public GameWindow(State state, GameLogic game, ProgressStore progressStore) {
        super("Sokoban");
        this.state = state;
        this.game = game;
        this.progressStore = progressStore;
        renderer = new LevelRenderer(state);

        bindMovementKeys();

        // Controls on top, the board in the middle, and the status panel along the bottom.
        setLayout(new BorderLayout());
        add(createControls(), BorderLayout.NORTH);
        add(renderer, BorderLayout.CENTER);
        configureStatusLabel();
        add(status, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));

        // Fill in the labels first so pack() can size the window to fit its contents,
        // then center it on the screen.
        refreshView();
        pack();
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------------------------

    /** WASD and the arrow keys both move the player. */
    private void bindMovementKeys() {
        bindMovement(KeyEvent.VK_W, Direction.UP);
        bindMovement(KeyEvent.VK_UP, Direction.UP);

        bindMovement(KeyEvent.VK_S, Direction.DOWN);
        bindMovement(KeyEvent.VK_DOWN, Direction.DOWN);

        bindMovement(KeyEvent.VK_A, Direction.LEFT);
        bindMovement(KeyEvent.VK_LEFT, Direction.LEFT);

        bindMovement(KeyEvent.VK_D, Direction.RIGHT);
        bindMovement(KeyEvent.VK_RIGHT, Direction.RIGHT);
    }

    /** Connect one key to a move. Swing needs two steps: name the key, then define the action. */
    private void bindMovement(int keyCode, Direction direction) {
        String actionName = "move-" + KeyEvent.getKeyText(keyCode);

        // The input map says "this key triggers the action with this name"...
        // WHEN_IN_FOCUSED_WINDOW makes the key work whenever this window has focus.
        renderer.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(keyCode, 0), actionName);

        // ...and the action map says "the action with this name runs this code".
        renderer.getActionMap().put(actionName, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent event) {
                handleMoveKey(direction);
            }
        });
    }

    /** Create the level picker and the buttons above the board. */
    private JPanel createControls() {
        fillLevelList();
        JButton goToLevel = new JButton("Go To Level");
        JButton resetLevel = new JButton("Reset Level");
        JButton generateLevel = new JButton("Generate Level");

        // Keep the controls from taking keyboard focus, so the arrow keys always move the
        // player instead of changing the dropdown.
        levelList.setFocusable(false);
        goToLevel.setFocusable(false);
        resetLevel.setFocusable(false);
        generateLevel.setFocusable(false);

        // Swing runs each of these blocks whenever the player clicks the button.
        goToLevel.addActionListener(event -> {
            int selectedIndex = levelList.getSelectedIndex();
            if (selectedIndex >= 0) { // JComboBox reports -1 when nothing is selected.
                loadLevel(selectedIndex);
            }
        });
        // Reset restarts the level on screen, ignoring any unconfirmed dropdown choice.
        resetLevel.addActionListener(event -> loadLevel(state.getCurrentLevelIndex()));
        generateLevel.addActionListener(event -> startGeneratedLevel());

        // FlowLayout keeps the controls together in a row with a small gap around each.
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controls.add(levelList);
        controls.add(goToLevel);
        controls.add(resetLevel);
        controls.add(generateLevel);
        return controls;
    }

    /** The bottom panel: a colored strip that shows the game's progress as centered lines. */
    private void configureStatusLabel() {
        status.setOpaque(true); // A label only paints its background when it is opaque.
        status.setBackground(Color.BLUE);
        status.setForeground(Color.YELLOW);
        status.setHorizontalAlignment(SwingConstants.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
    }

    // ---------------------------------------------------------------------------------
    // Reacting to the player
    // ---------------------------------------------------------------------------------

    /** Handle a movement key: one step by the player. */
    private void handleMoveKey(Direction direction) {
        boolean wasCompleted = game.isAttemptCompleted();
        game.moveCharacter(direction.dr(), direction.dc());
        // Only the move that finishes the level counts. GameLogic ignores later moves.
        if (!wasCompleted && game.isAttemptCompleted()) {
            handleCompletion();
        }
        refreshView(); // Redraw even if the move was blocked, so the screen is always current.
    }

    /** Start the given level from scratch. */
    private void loadLevel(int index) {
        game.selectLevel(index);
        refreshView();
    }

    /** Make a brand new level and start playing it. */
    private void startGeneratedLevel() {
        LevelGenerator.GeneratedLevel level = generator.generate();
        game.playGeneratedLevel(level.rows()); // The solution inside is not shown to the player.
        fillLevelList(); // Add the "Generated level" entry if this is the first one.
        refreshView();
    }

    /** Called when a level is finished: update the dropdown and save the result. */
    private void handleCompletion() {
        // Generated levels are not saved, so there is nothing to update.
        if (state.isGeneratedLevel(state.getCurrentLevelIndex())) {
            return;
        }
        fillLevelList(); // Add the "(completed)" marker.
        saveProgress();
    }

    /** Save the records right away, so closing the window cannot lose a result. */
    private void saveProgress() {
        try {
            progressStore.save(game.getProgress());
        } catch (IOException exception) {
            // The records stay in memory, but never claim they were saved when writing failed.
            JOptionPane.showMessageDialog(this,
                    "Level completed, but progress could not be saved.\n"
                            + "Your records remain available for this session.\n"
                            + exception.getMessage(),
                    "Could not save progress", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ---------------------------------------------------------------------------------
    // Updating the display
    // ---------------------------------------------------------------------------------

    /** Call after any game action so everything on screen matches the shared state. */
    private void refreshView() {
        // Show the level that is actually loaded, not an unconfirmed dropdown choice.
        levelList.setSelectedIndex(state.getCurrentLevelIndex());
        status.setText(buildStatusHtml());
        // Ask Swing to repaint later on the event thread; never draw directly from here.
        renderer.repaint();
    }

    /** The status panel text. Labels use HTML so each line can be centered separately. */
    private String buildStatusHtml() {
        int levelIndex = state.getCurrentLevelIndex();
        boolean generated = state.isGeneratedLevel(levelIndex);
        // A generated level has no saved record, because it is not in the level file.
        PlayerRecord record = null;
        if (!generated) {
            record = game.getProgress().getRecord(game.getLevelId(levelIndex));
        }
        String levelText = "Generated level";
        if (!generated) {
            levelText = "Level " + (levelIndex + 1) + " of " + state.getSavedLevelCount();
        }
        String bestScore = record == null ? "--" : Integer.toString(record.bestScore());
        String fewestMoves = record == null ? "--" : Integer.toString(record.fewestMoves());
        String solvedNote = game.isAttemptCompleted() ? "Solved! Reset or choose a level." : "";

        // Every line always exists, using a blank placeholder when empty, so the panel keeps
        // the same height and the board does not jump around as messages appear.
        List<String> lines = new ArrayList<>();
        lines.add(levelText);
        lines.add("Moves: " + state.getMoveCount());
        lines.add("Score: " + state.getScore());
        lines.add("Best score: " + bestScore);
        lines.add("Fewest moves: " + fewestMoves);
        lines.add(orBlank(solvedNote));

        return "<html><div style='text-align: center;'>" + String.join("<br>", lines)
                + "</div></html>";
    }

    /** Swap empty text for a non-breaking space, which HTML still gives a line of height. */
    private static String orBlank(String text) {
        return text.isEmpty() ? "&nbsp;" : text;
    }

    /** Fill the dropdown with one entry per level, keeping the current selection. */
    private void fillLevelList() {
        int selected = levelList.getSelectedIndex(); // -1 on the first call, when it is empty.
        levelList.removeAllItems();
        for (int index = 0; index < state.getLevelCount(); index++) {
            levelList.addItem(levelLabel(index));
        }
        if (selected >= 0) {
            levelList.setSelectedIndex(selected);
        }
    }

    /** "Level 4", with "(completed)" added if the player has finished it before. */
    private String levelLabel(int index) {
        if (state.isGeneratedLevel(index)) {
            return "Generated level";
        }
        String label = "Level " + (index + 1); // Players count from 1; the model counts from 0.
        if (game.getProgress().isCompleted(game.getLevelId(index))) {
            label += " (completed)";
        }
        return label;
    }
}
