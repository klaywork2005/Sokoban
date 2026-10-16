package sokoban;


import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
  public static void main(String[] args) {
    
    try {
      LevelLoader loader = new LevelLoader();

      Path path = Path.of("src", "main", "resources", "original-levels.txt");
      
      
    
      List<List<String>> levels = loader.loadLevels(path);

      if (levels.isEmpty()) {
        System.out.println("No levels found.");
        return;
      }
    SwingUtilities.invokeLater(() -> {
      JFrame frame = new JFrame("Sokoban");

      LevelRenderer panel = new LevelRenderer(levels.get(0));
      
      panel.setPreferredSize(new Dimension(1280, 720));
      
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.add(panel);
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
      });
    
    }catch (IOException e) {
      System.out.println("Couldn't open file: " + e.getMessage());
    }
  } 
}
