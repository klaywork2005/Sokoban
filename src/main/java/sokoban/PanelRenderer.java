package sokoban;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class PanelRenderer extends JPanel {
  


  public PanelRenderer() {
      setBackground(Color.BLUE);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    
    Graphics2D g2 = (Graphics2D) g;

    g2.setColor(Color.DARK_GRAY);
    g2.fillRect(100, 100, 64, 64);
  }
}