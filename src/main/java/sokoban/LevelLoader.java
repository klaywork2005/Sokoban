package sokoban;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class LevelLoader {
  File originalLevelsFile = new File("Sokoban/main/resources/original-levels.txt");
  File microbanLevelsFile = new File("Sokoban/main/resources/microban.txt");

  {
  try (Scanner levelsReader = new Scanner(originalLevelsFile)) {
    while (levelsReader.hasNextLine()) {
      String data = levelsReader.nextLine();
      System.out.println(data);
    }
  } catch (FileNotFoundException e) {
    System.out.println("An error occurred.");
    e.printStackTrace();
    }
  }}



