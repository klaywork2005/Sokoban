package sokoban;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The first screen the player sees: the name of the game, a Play button and a Quit button.
 * It does not know what the buttons do. The window passes in the code to run for each.
 */
public final class TitleScreen extends JPanel {
    // Swing components are Serializable, so the compiler expects a version number.
    // This panel is never actually saved to disk.
    private static final long serialVersionUID = 1L;

    private static final Color TITLE_COLOR = new Color(255, 214, 79);
    private static final Color TEXT_COLOR = new Color(240, 230, 210);
    // The dark box behind the text. The last number makes it a little see-through.
    private static final Color BOX_COLOR = new Color(35, 24, 18, 215);

    private static final int TITLE_SIZE = 72;
    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 48;
    // The artwork is drawn this many times bigger than in the game.

    // Only used for the artwork, which this screen shares with the game board.
    private final WarehouseSprites sprites = new WarehouseSprites();
    private final JButton playButton = new JButton("Play");

    /**
     * @param onPlay code to run when the player clicks Play
     * @param onQuit code to run when the player clicks Quit
     */
    public TitleScreen(Runnable onPlay, Runnable onQuit) {
        // GridBagLayout puts a single component in the middle of the panel.
        setLayout(new GridBagLayout());
        add(createBox(onPlay, onQuit));
    }

    /** The Play button, so that the window can make the Enter key press it. */
    JButton getPlayButton() {
        return playButton;
    }

    /** Stack the picture, the title, a short line of help, and the buttons from top to bottom. */
    private JPanel createBox(Runnable onPlay, Runnable onQuit) {
        JPanel box = new TextBox();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));

        JLabel title = createLabel("SOKOBAN", new Font(Font.SANS_SERIF, Font.BOLD, TITLE_SIZE),
                TITLE_COLOR);
        JLabel tagline = createLabel("Push every box onto a goal.",
                new Font(Font.SANS_SERIF, Font.PLAIN, 22), TEXT_COLOR);
        JLabel help = createLabel("Move with W A S D or the arrow keys. Press Z to undo.",
                new Font(Font.SANS_SERIF, Font.PLAIN, 16), TEXT_COLOR);

        styleButton(playButton);
        JButton quitButton = new JButton("Quit");
        styleButton(quitButton);
        // Swing runs each of these blocks whenever the player clicks the button.
        playButton.addActionListener(event -> onPlay.run());
        quitButton.addActionListener(event -> onQuit.run());

        box.add(Box.createVerticalStrut(10));
        box.add(title);
        box.add(Box.createVerticalStrut(6));
        box.add(tagline);
        box.add(Box.createVerticalStrut(30));
        box.add(playButton);
        box.add(Box.createVerticalStrut(12));
        box.add(quitButton);
        box.add(Box.createVerticalStrut(30));
        box.add(help);
        return box;
    }

    private static JLabel createLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT); // Centered inside the box.
        return label;
    }

    private static void styleButton(JButton button) {
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        // BoxLayout respects the maximum size, so set all three to make the button this size.
        Dimension size = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
    }

    

    /** Cover the whole panel with the same concrete floor that surrounds the game board. */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        int tile = WarehouseSprites.TILE_SIZE;
        for (int y = 0; y < getHeight(); y += tile) {
            for (int x = 0; x < getWidth(); x += tile) {
                graphics.drawImage(sprites.concrete, x, y, null);
            }
        }
    }

    /** A panel with a dark, rounded, slightly see-through background. */
    private static final class TextBox extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final int CORNER = 30;

        TextBox() {
            // We paint our own background, so tell Swing not to paint a plain one first.
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            graphics.setColor(BOX_COLOR);
            graphics.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER, CORNER);
            super.paintComponent(graphics);
        }
    }
}
