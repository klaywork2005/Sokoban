package sokoban;

import java.util.List;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Dimension;

public class LevelRenderer extends JPanel {
  private static final int TILE_SIZE = 48;
  private List<String> level;

  public LevelRenderer(List<String> level) {
    this.level = level;
    setBackground(Color.BLACK);

  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    int columns = 0;

    for (String line : level) {
        columns = Math.max(columns, line.length());
    }

    int offsetX = (getWidth() - columns * TILE_SIZE) / 2;
    int offsetY = (getHeight() - level.size() * TILE_SIZE) / 2;
    for (int row = 0; row < level.size(); row++){
      String line = level.get(row);

      for (int col = 0; col < line.length(); col++) {
        char tile = line.charAt(col);

        int x = offsetX + col * TILE_SIZE;
        int y = offsetY + row * TILE_SIZE;

        // Draw floor underneath the goal, box, or player.
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
        
        /* #  wall     (space) floor     .  goal
           $  box      @  player         *  box on goal     +  player on goal
        */

        // *  box on goal
        if (tile == '*') {
          // Leave the goal ring visible around the box.
          g.setColor(Color.RED);
          g.fillRect(x + 8, y + 8, TILE_SIZE - 16, TILE_SIZE - 16);
        }
        else if (tile == '+') {
          g.setColor(Color.CYAN);
          g.drawOval(x + 4, y + 4, TILE_SIZE - 8, TILE_SIZE - 8);
          g.setColor(Color.YELLOW);
          g.fillOval(x + TILE_SIZE/4, y + TILE_SIZE/4, TILE_SIZE/2, TILE_SIZE/2);
        }
        else if (tile == '$') {
          g.setColor(Color.GREEN);
          g.fillRect(x + 8, y + 8, TILE_SIZE - 16, TILE_SIZE - 16);
        }
        else if (tile == '@') {
          g.setColor(Color.YELLOW);
          g.fillOval(x + TILE_SIZE/4, y + TILE_SIZE/4, TILE_SIZE/2, TILE_SIZE/2);
        } 
        else if (tile == '#') {
          g.setColor(Color.DARK_GRAY);
          g.fillRect(x,y, TILE_SIZE, TILE_SIZE);
        } 
        else if (tile == '.' ) {
          g.setColor(Color.CYAN);
          g.fillOval(x + TILE_SIZE/3, y + TILE_SIZE/3, TILE_SIZE/3, TILE_SIZE/3);
        }
      }
    }
  }

  public void setLevel(List<String> level) {
    this.level = level;
    repaint();
  }
}
