package sokoban;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

    // The window shows one of these two screens at a time.
    private static final String TITLE_SCREEN = "title";
    private static final String GAME_SCREEN = "game";

    // The window shares the model and rules; it only coordinates input and display.
    private final State state;
    private final GameLogic game;
    private final LevelRenderer renderer;
    private final ProgressStore progressStore;
    private final BackgroundMusic music;
    private final SoundEffects sounds = new SoundEffects();
    // A new Random() gives different levels every time the program runs.
    private final LevelGenerator generator = new LevelGenerator(new Random());

    // A CardLayout shows one of its panels at a time, like a stack of cards.
    private final CardLayout screens = new CardLayout();
    private final JPanel screenPanel = new JPanel(screens);
    private final TitleScreen titleScreen = new TitleScreen(this::showGame, this::quit);
    // True until the player clicks Play. Keys must not move anything while the title shows.
    private boolean onTitleScreen = true;

    // The dropdown can hold a pending choice until "Go To Level" is clicked.
    private final JComboBox<String> levelList = new JComboBox<>();
    private final JComboBox<Difficulty> difficultyList = new JComboBox<>(Difficulty.values());
    // Fields (not local variables) because refreshView() switches Undo on and off.
    private final JButton undoButton = new JButton("Undo");
    private final JCheckBox musicBox = new JCheckBox("Music", true);
    private final JLabel status = new JLabel();

    /**
     * Build the window. {@code Main} calls this on Swing's event thread.
     *
     * @param progressStore where completed levels are saved
     * @param music         the background music, so the Music box can silence it
     */
    public GameWindow(State state, GameLogic game, ProgressStore progressStore,
            BackgroundMusic music) {
        super("Sokoban");
        this.state = state;
        this.game = game;
        this.progressStore = progressStore;
        this.music = music;
        renderer = new LevelRenderer(state);

        bindMovementKeys();
        bindShortcutKeys();

        // The game screen: controls on top, the board in the middle, and the status panel
        // along the bottom.
        JPanel gameScreen = new JPanel(new BorderLayout());
        JPanel controls = createControls();
        gameScreen.add(controls, BorderLayout.NORTH);
        gameScreen.add(renderer, BorderLayout.CENTER);
        configureStatusLabel();
        gameScreen.add(status, BorderLayout.SOUTH);

        // The title screen is on top until the player clicks Play. Enter presses Play too.
        screenPanel.add(titleScreen, TITLE_SCREEN);
        screenPanel.add(gameScreen, GAME_SCREEN);
        add(screenPanel);
        getRootPane().setDefaultButton(titleScreen.getPlayButton());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Never let the window get narrower than its row of controls, or they would be cut off.
        int minWidth = Math.max(MIN_WIDTH, controls.getPreferredSize().width);
        setMinimumSize(new Dimension(minWidth, MIN_HEIGHT));

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

    /** Keyboard shortcuts for the buttons, so a game can be played without the mouse. */
    private void bindShortcutKeys() {
        // Several keys can run the same action: they just share an action name.
        bindKey(KeyEvent.VK_Z, 0, "undo", this::undoMove);
        bindKey(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK, "undo", this::undoMove);
        bindKey(KeyEvent.VK_BACK_SPACE, 0, "undo", this::undoMove);
        bindKey(KeyEvent.VK_R, 0, "restart", this::restartLevel);
        bindKey(KeyEvent.VK_N, 0, "next-level", this::goToNextLevel);
        bindKey(KeyEvent.VK_P, 0, "previous-level", this::goToPreviousLevel);
        // Clicking the check box in code does exactly what a mouse click would do.
        bindKey(KeyEvent.VK_M, 0, "toggle-music", musicBox::doClick);
    }

    /** Connect one movement key to a step in a direction. */
    private void bindMovement(int keyCode, Direction direction) {
        bindKey(keyCode, 0, "move-" + KeyEvent.getKeyText(keyCode),
                () -> handleMoveKey(direction));
    }

    /**
     * Connect one key to some code. Swing needs two steps: name the key, then define the action.
     *
     * @param modifiers extra keys that must be held down, such as
     *                  {@code InputEvent.CTRL_DOWN_MASK}, or 0 for the key on its own
     */
    private void bindKey(int keyCode, int modifiers, String actionName, Runnable action) {
        // The input map says "this key triggers the action with this name"...
        // WHEN_IN_FOCUSED_WINDOW makes the key work whenever this window has focus.
        renderer.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(keyCode, modifiers), actionName);

        // ...and the action map says "the action with this name runs this code".
        renderer.getActionMap().put(actionName, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent event) {
                // Keys must not change anything while the title screen is showing.
                if (!onTitleScreen) {
                    action.run();
                }
            }
        });
    }

    /** Create the level picker and the buttons above the board. */
    private JPanel createControls() {
        fillLevelList();
        JButton goToLevel = new JButton("Go To Level");
        JButton resetLevel = new JButton("Reset Level");
        JButton nextLevel = new JButton("Next Level");
        JButton generateLevel = new JButton("Generate Level");

        // With over 200 levels the list needs to scroll instead of covering the screen.
        levelList.setMaximumRowCount(15);
        difficultyList.setSelectedItem(Difficulty.MEDIUM);

        // Keep the controls from taking keyboard focus, so the arrow keys always move the
        // player instead of changing the dropdown.
        levelList.setFocusable(false);
        difficultyList.setFocusable(false);
        goToLevel.setFocusable(false);
        resetLevel.setFocusable(false);
        undoButton.setFocusable(false);
        nextLevel.setFocusable(false);
        generateLevel.setFocusable(false);
        musicBox.setFocusable(false);

        // A tooltip appears when the mouse rests on a control. These teach the shortcut keys.
        resetLevel.setToolTipText("Start this level again (R)");
        undoButton.setToolTipText("Take back the last move (Z, Ctrl+Z or Backspace)");
        nextLevel.setToolTipText("Go to the next level (N). P goes back one level.");
        generateLevel.setToolTipText("Make a new level at the chosen difficulty");
        musicBox.setToolTipText("Turn the music on or off (M)");

        // Swing runs each of these blocks whenever the player clicks the button.
        goToLevel.addActionListener(event -> {
            int selectedIndex = levelList.getSelectedIndex();
            if (selectedIndex >= 0) { // JComboBox reports -1 when nothing is selected.
                loadLevel(selectedIndex);
            }
        });
        // Reset restarts the level on screen, ignoring any unconfirmed dropdown choice.
        resetLevel.addActionListener(event -> restartLevel());
        undoButton.addActionListener(event -> undoMove());
        nextLevel.addActionListener(event -> goToNextLevel());
        generateLevel.addActionListener(event -> startGeneratedLevel());
        musicBox.addActionListener(event -> music.setMuted(!musicBox.isSelected()));

        // Two rows of controls: the levels on top and the extras below. FlowLayout keeps the
        // controls of one row together, with a small gap around each.
        JPanel levelRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        levelRow.add(levelList);
        levelRow.add(goToLevel);
        levelRow.add(resetLevel);
        levelRow.add(undoButton);
        levelRow.add(nextLevel);

        JPanel extrasRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        extrasRow.add(generateLevel);
        extrasRow.add(new JLabel("Difficulty:"));
        extrasRow.add(difficultyList);
        extrasRow.add(musicBox);

        JPanel controls = new JPanel(new GridLayout(2, 1)); // Two equal rows, one above the other.
        controls.add(levelRow);
        controls.add(extrasRow);
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

    /** Leave the title screen and start playing. */
    private void showGame() {
        onTitleScreen = false;
        // The Play button is hidden now, so the Enter key must stop pressing it.
        getRootPane().setDefaultButton(null);
        screens.show(screenPanel, GAME_SCREEN);
    }

    /** Close the window and end the program. */
    private void quit() {
        dispose();
        System.exit(0);
    }

    /** Handle a movement key: one step by the player. */
    private void handleMoveKey(Direction direction) {
        boolean wasCompleted = game.isAttemptCompleted();
        game.moveCharacter(direction.dr(), direction.dc());
        // Only the move that finishes the level counts. GameLogic ignores later moves.
        if (!wasCompleted && game.isAttemptCompleted()) {
            sounds.playLevelComplete();
            handleCompletion();
        }
        refreshView(); // Redraw even if the move was blocked, so the screen is always current.
    }

    /** Take back the last move, if there is one. */
    private void undoMove() {
        game.undoMove();
        refreshView();
    }

    /** Start the given level from scratch. */
    private void loadLevel(int index) {
        game.selectLevel(index);
        refreshView();
    }

    /** Start the level on screen over again. */
    private void restartLevel() {
        loadLevel(state.getCurrentLevelIndex());
    }

    /** Move on to the next level in the list. Nothing happens at the last level. */
    private void goToNextLevel() {
        game.selectNextLevel();
        refreshView();
    }

    /** Go back to the previous level in the list. Nothing happens at the first level. */
    private void goToPreviousLevel() {
        game.selectPreviousLevel();
        refreshView();
    }

    /** Make a brand new level, as hard as the Difficulty box says, and start playing it. */
    private void startGeneratedLevel() {
        // A combo box hands back a plain Object, so we tell Java it holds a Difficulty.
        Difficulty difficulty = (Difficulty) difficultyList.getSelectedItem();
        LevelGenerator.GeneratedLevel level = generator.generate(difficulty);
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
        undoButton.setEnabled(game.canUndo()); // Grey out Undo when there is nothing to take back.
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
            // For example "Microban 12 (63 of 206)": its place in its own file, then overall.
            levelText = game.getLevelName(levelIndex)
                    + " (" + (levelIndex + 1) + " of " + state.getSavedLevelCount() + ")";
        }
        String bestScore = record == null ? "--" : Integer.toString(record.bestScore());
        String fewestMoves = record == null ? "--" : Integer.toString(record.fewestMoves());

        // Every line always exists, using a blank placeholder when empty, so the panel keeps
        // the same height and the board does not jump around as messages appear.
        List<String> lines = new ArrayList<>();
        lines.add(levelText);
        lines.add("Moves: " + state.getMoveCount());
        lines.add("Score: " + state.getScore());
        lines.add("Best score: " + bestScore);
        lines.add("Fewest moves: " + fewestMoves);
        lines.add(orBlank(buildNote()));

        return "<html><div style='text-align: center;'>" + String.join("<br>", lines)
                + "</div></html>";
    }

    /** The message on the last line of the status panel, or empty text if there is none. */
    private String buildNote() {
        if (game.isAttemptCompleted()) {
            boolean lastLevel = state.getCurrentLevelIndex() + 1 >= state.getLevelCount();
            if (lastLevel) {
                return "Solved! Reset or choose a level.";
            }
            return "Solved! Press N for the next level.";
        }
        return "";
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

    /** "Microban 4", with "(completed)" added if the player has finished it before. */
    private String levelLabel(int index) {
        if (state.isGeneratedLevel(index)) {
            return "Generated level";
        }
        String label = game.getLevelName(index);
        if (game.getProgress().isCompleted(game.getLevelId(index))) {
            label += " (completed)";
        }
        return label;
    }
}
