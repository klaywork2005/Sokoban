package sokoban;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

/** Owns the controls and connects user input to game actions. */
public final class GameWindow extends JFrame {
  // Share the model and rules; this class only coordinates UI actions and display.
  private final State state;
  private final GameLogic game;
  private final LevelRenderer renderer;
  // The dropdown can hold a pending selection until Go To Level is clicked.
  private final JComboBox<String> levelList = new JComboBox<>();
  private final JLabel status = new JLabel();

  /** Build the window on Swing's event dispatch thread, as scheduled by Main. */
  public GameWindow(State state, GameLogic game) {
    super("Sokoban");
    this.state = state;
    this.game = game;
    renderer = new LevelRenderer(state);
    
    // Each direction has a WASD binding and an arrow binding to the same move.
    bindMovement(KeyEvent.VK_W, "w", -1, 0);
    bindMovement(KeyEvent.VK_UP, "up", -1, 0);

    bindMovement(KeyEvent.VK_S, "s", 1, 0);
    bindMovement(KeyEvent.VK_DOWN, "down", 1, 0);

    bindMovement(KeyEvent.VK_A, "a", 0, -1);
    bindMovement(KeyEvent.VK_LEFT, "left", 0, -1);

    bindMovement(KeyEvent.VK_D, "d", 0, 1);
    bindMovement(KeyEvent.VK_RIGHT, "right", 0, 1);
    
    // Place controls above the board and the active-level status below it.
    setLayout(new BorderLayout());
    add(createControls(), BorderLayout.NORTH);
    add(renderer, BorderLayout.CENTER);
    add(status, BorderLayout.SOUTH);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setMinimumSize(new Dimension(600, 450));
    // Initialize labels, size the frame to its contents, and center it on screen.
    refreshView();
    pack();
    setLocationRelativeTo(null);
  }

  /** Connect a keystroke to a named Swing action with a fixed movement offset. */
  private void bindMovement(int keyCode, String actionName, int dr, int dc) {
    // Window-wide bindings work whenever this window has keyboard focus.
    renderer.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0), actionName);

    // The input map names the action; the action map supplies the code to run.
    renderer.getActionMap().put(actionName, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent event) {
        // Repaint after every attempt, including attempts blocked by a wall or box.
        game.moveCharacter(dr, dc);
        refreshView();
      }
    });
  }

  /** Create the level picker and the button that commits its pending selection. */
  private JPanel createControls() {
    // Human-readable level numbers start at one, while model indices start at zero.
    for (int index = 0; index < state.getLevelCount(); index++) {
      levelList.addItem("Level " + (index + 1));
    }
    JButton confirmLevel = new JButton("Go To Level");
    // Keep arrow keys available for movement instead of changing controls.
    levelList.setFocusable(false);
    confirmLevel.setFocusable(false);
    // Swing runs this block each time the player clicks the button.
    confirmLevel.addActionListener(event -> {
      int selectedIndex = levelList.getSelectedIndex();
      // JComboBox reports -1 when there is no selected item.
      if (selectedIndex >= 0) {
        game.selectLevel(selectedIndex);
        refreshView();
      }
    });
    // FlowLayout keeps the two controls together with a small gap around them.
    JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
    controls.add(levelList);
    controls.add(confirmLevel);
    return controls;
  }

  /**
   * Call after a game action so every displayed value reflects the shared state.
   */
  private void refreshView() {
    // Reflect the loaded level rather than any unconfirmed dropdown selection.
    levelList.setSelectedIndex(state.getCurrentLevelIndex());
    status.setText("Level " + (state.getCurrentLevelIndex() + 1) + " of " + state.getLevelCount());
    // Request a future paint on Swing's event thread; do not draw directly here.
    renderer.repaint();
  }
}
