package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;

/** Check that the title screen shows the game's name and that its buttons do their jobs. */
class TitleScreenTest {
    /** Collect every component of one kind inside a container, however deeply it is nested. */
    private static <T> List<T> findAll(Container container, Class<T> kind) {
        List<T> found = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (kind.isInstance(child)) {
                found.add(kind.cast(child));
            }
            if (child instanceof Container inner) {
                found.addAll(findAll(inner, kind));
            }
        }
        return found;
    }

    private static JButton findButton(TitleScreen screen, String text) {
        for (JButton button : findAll(screen, JButton.class)) {
            if (button.getText().equals(text)) {
                return button;
            }
        }
        throw new AssertionError("No button called " + text);
    }

    @Test
    void clickingPlayRunsThePlayCodeOnly() {
        int[] clicks = new int[2]; // [0] counts Play, [1] counts Quit.
        TitleScreen screen = new TitleScreen(() -> clicks[0]++, () -> clicks[1]++);

        findButton(screen, "Play").doClick();

        assertEquals(1, clicks[0]);
        assertEquals(0, clicks[1]);
    }

    @Test
    void clickingQuitRunsTheQuitCodeOnly() {
        int[] clicks = new int[2];
        TitleScreen screen = new TitleScreen(() -> clicks[0]++, () -> clicks[1]++);

        findButton(screen, "Quit").doClick();

        assertEquals(0, clicks[0]);
        assertEquals(1, clicks[1]);
    }

    @Test
    void theWindowCanGetHoldOfThePlayButton() {
        TitleScreen screen = new TitleScreen(() -> { }, () -> { });

        assertEquals("Play", screen.getPlayButton().getText());
    }

    @Test
    void theGamesNameIsShown() {
        TitleScreen screen = new TitleScreen(() -> { }, () -> { });

        boolean foundTitle = false;
        for (JLabel label : findAll(screen, JLabel.class)) {
            if ("SOKOBAN".equals(label.getText())) {
                foundTitle = true;
            }
        }
        assertTrue(foundTitle);
    }
}
