package sokoban;


import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.swing.*;
import java.awt.*;


public class Main {
  public static void main(String[] args) {
    
    try {

      Path path = Path.of("src", "main", "resources", "original-levels.txt");
      
      LevelLoader loader = new LevelLoader();
      
    
      List<List<String>> levels = loader.loadLevels(path);

      if (levels.isEmpty()) {
        System.out.println("No levels found.");
        return;
      }

    SwingUtilities.invokeLater(() -> {
      JFrame frame = new JFrame("Sokoban");
      frame.setLayout(new BorderLayout());

      // Create the map panel with the first level.
      LevelRenderer panel = new LevelRenderer(levels.get(0));
      panel.setPreferredSize(new Dimension(1100, 900));
      // Show one entry for each loaded level.
      JComboBox<String> levelList = new JComboBox<>();
      levelList.setBorder(BorderFactory.createLineBorder(new Color(52, 152, 219), 2));
      levelList.setPreferredSize(new Dimension(150, 35));

      for (int i = 0; i < levels.size(); i++) {
          levelList.addItem("Level " + (i + 1));
      }
      
      levelList.setSelectedIndex(0);

      JButton confirmLevel = new JButton("Go To Level");
      confirmLevel.setBackground(new Color(200, 200, 200));
      confirmLevel.setForeground(Color.BLACK);
      confirmLevel.setFont(new Font("Arial", Font.BOLD, 16));
      confirmLevel.setFocusPainted(false);
      confirmLevel.setOpaque(true);

      // Read the selection when the button is clicked.
      confirmLevel.addActionListener(event -> {
          int selectedIndex = levelList.getSelectedIndex();

          if (selectedIndex >= 0) {
              panel.setLevel(levels.get(selectedIndex));
          }
      });

      // Center the controls side by side across the top of the window.
      JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
      controls.setBackground(Color.BLUE);
      controls.add(levelList);
      controls.add(confirmLevel);
            
      
      
      frame.setMinimumSize(new Dimension(1100,900));
      frame.add(controls, BorderLayout.NORTH);
      frame.add(panel, BorderLayout.CENTER);

      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);}
    );
    }catch (IOException e) {
      System.out.println("Couldn't open file: " + e.getMessage());
    }
  } 
}
