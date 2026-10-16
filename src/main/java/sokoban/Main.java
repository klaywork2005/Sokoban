package sokoban;

import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        
      LevelLoader originalFile = new LevelLoader();
        System.out.println(originalFile);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Sokoban");
            PanelRenderer panel = new PanelRenderer();

            panel.setPreferredSize(new Dimension(640, 480));

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}