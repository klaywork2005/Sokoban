package sokoban;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Makes new Sokoban levels that can always be solved.
 *
 * <p>
 * The trick is to build a level by playing it backwards. We start with a
 * finished level,
 * where every box is already on a goal, and then undo pushes one at a time: the
 * player walks
 * next to a box and pulls it one square. Each undone push can be played
 * forwards again, so
 * doing all the pulls in reverse order solves the level. That is why the level
 * can never
 * be impossible.
 *
 * <p>
 * The game never shows those solving moves. They are returned only so that the
 * tests can
 * replay them and prove that a level really can be finished.
 *
 * <p>
 * A generator keeps working notes in its fields while it builds a level, so it
 * should not
 * be used by two threads at once.
 */
public final class LevelGenerator {
  // The difficulty of the levels made by generate() with no arguments.
  private static final Difficulty DEFAULT_DIFFICULTY = Difficulty.MEDIUM;

  // A level that can be solved in fewer steps than this is too easy to be worth
  // playing.
  private static final int MIN_SOLUTION_STEPS = 12;
  // How many times to start over if a level comes out unusable, before giving up.
  private static final int MAX_TRIES = 100;
  // A random pull often fails (a wall is in the way, for example), so allow many
  // attempts.
  private static final int ATTEMPTS_PER_PULL = 50;

  /**
   * A finished level.
   *
   * @param rows     the board, one string per row, in the usual level symbols
   * @param solution the moves that solve it, one direction per step of the player
   */
  public record GeneratedLevel(List<String> rows, List<Direction> solution) {
  }

  /**
   * A spot on the board. A record is a tiny class that just holds some values.
   */
  private record Square(int row, int column) {
    /** The square one step away in the given direction. */
    Square next(Direction direction) {
      return new Square(row + direction.dr(), column + direction.dc());
    }
  }

  /**
   * One undone push, remembered so the solution can be written out later.
   *
   * @param walk          how the player walked to the square next to the box
   * @param pushDirection the way the box was pushed originally (it was pulled
   *                      back the other way)
   */
  private record Pull(List<Direction> walk, Direction pushDirection) {
  }

  private final Random random;

  // The notes below describe the level being built. generate() starts them afresh
  // each try.
  private boolean[][] wall;
  private List<Square> goals;
  private List<Square> boxes;
  private Square player;

  /**
   * @param random the source of all random choices; use a fixed seed to get the
   *               same levels
   */
  public LevelGenerator(Random random) {
    this.random = random;
  }

  /** Make a level of the standard size and difficulty (medium). */
  public GeneratedLevel generate() {
    return generate(DEFAULT_DIFFICULTY);
  }

  /**
   * Make a level with the room size, box count and number of pulls of a
   * difficulty setting.
   */
  public GeneratedLevel generate(Difficulty difficulty) {
    return generate(difficulty.width(), difficulty.height(), difficulty.boxes(),
        difficulty.walls(), difficulty.pulls());
  }

  /**
   * Make a level.
   *
   * @param width     columns, including the outer wall (at least 5)
   * @param height    rows, including the outer wall (at least 5)
   * @param boxCount  how many boxes (and goals) the level has
   * @param wallCount how many extra walls to try to place inside the room. A wall
   *                  that
   *                  would cut the room in two is skipped, so the level may get
   *                  fewer.
   * @param pullCount how many pushes to undo. More pulls mixes the boxes up
   *                  further,
   *                  which makes a harder level.
   * @throws IllegalArgumentException if the numbers are too small, or the boxes
   *                                  cannot fit
   * @throws IllegalStateException    if no usable level could be made, for
   *                                  example because
   *                                  the room is so small that every level is too
   *                                  easy
   */
  public GeneratedLevel generate(int width, int height, int boxCount, int wallCount,
      int pullCount) {
    int roomSquares = (width - 2) * (height - 2);
    boolean tooSmall = width < 5 || height < 5 || boxCount < 1 || wallCount < 0
        || pullCount < 1;
    // Besides the walls we need room for every box and for the player.
    if (tooSmall || boxCount + 1 + wallCount > roomSquares) {
      throw new IllegalArgumentException("Those settings do not make a playable level.");
    }

    for (int attempt = 0; attempt < MAX_TRIES; attempt++) {
      GeneratedLevel level = tryToGenerate(width, height, boxCount, wallCount, pullCount);
      if (level != null) {
        return level;
      }
    }
    throw new IllegalStateException("Could not make a level with those settings.");
  }

  /** One try at making a level. Returns null if the result is unusable. */
  private GeneratedLevel tryToGenerate(int width, int height, int boxCount, int wallCount,
      int pullCount) {
    buildRoom(width, height, wallCount);
    placeBoxesAndPlayer(boxCount);
    List<Pull> pulls = undoPushes(pullCount);

    // Random pulls sometimes cancel each other out and leave boxes where they
    // began.
    // A level where most boxes are already home is too easy, so start over instead.
    if (countBoxesOffGoals() * 2 < boxCount) {
      return null;
    }

    List<Direction> solution = buildSolution(pulls);
    if (solution.size() < MIN_SOLUTION_STEPS) {
      return null; // Too easy: start over.
    }
    return new GeneratedLevel(List.copyOf(buildRows()), List.copyOf(solution));
  }

  // ---------------------------------------------------------------------------------
  // Step 1: the room
  // ---------------------------------------------------------------------------------

  /** Make an empty room with a wall all around it, then add some walls inside. */
  private void buildRoom(int width, int height, int wallCount) {
    wall = new boolean[height][width];
    for (int row = 0; row < height; row++) {
      for (int column = 0; column < width; column++) {
        boolean onEdgeRow = row == 0 || row == height - 1;
        boolean onEdgeColumn = column == 0 || column == width - 1;
        wall[row][column] = onEdgeRow || onEdgeColumn;
      }
    }
    goals = new ArrayList<>();
    boxes = new ArrayList<>();

    for (int count = 0; count < wallCount; count++) {
      int row = 1 + random.nextInt(height - 2);
      int column = 1 + random.nextInt(width - 2);
      if (wall[row][column]) {
        continue; // Already a wall, so this attempt does nothing.
      }
      wall[row][column] = true;
      if (!roomIsConnected()) {
        wall[row][column] = false; // That wall cut the room in two, so take it back.
      }
    }
  }

  /** True if the player could walk from any floor square to any other. */
  private boolean roomIsConnected() {
    Square first = null;
    for (int row = 0; row < wall.length; row++) {
      for (int column = 0; column < wall[row].length; column++) {
        if (!wall[row][column] && first == null) {
          first = new Square(row, column);
        }
      }
    }
    if (first == null) {
      return false; // There is no floor at all.
    }
    for (Square square : allFloorSquares()) {
      if (findPath(first, square) == null) {
        return false;
      }
    }
    return true;
  }

  private List<Square> allFloorSquares() {
    List<Square> floor = new ArrayList<>();
    for (int row = 0; row < wall.length; row++) {
      for (int column = 0; column < wall[row].length; column++) {
        if (!wall[row][column]) {
          floor.add(new Square(row, column));
        }
      }
    }
    return floor;
  }

  // ---------------------------------------------------------------------------------
  // Step 2: the finished level
  // ---------------------------------------------------------------------------------

  /**
   * Put each box on its own goal at a random spot, and the player on another
   * random spot.
   */
  private void placeBoxesAndPlayer(int boxCount) {
    List<Square> floor = allFloorSquares();
    Collections.shuffle(floor, random);

    goals = new ArrayList<>(floor.subList(0, boxCount));
    boxes = new ArrayList<>(goals); // At first every box sits on a goal.
    player = floor.get(boxCount);
  }

  // ---------------------------------------------------------------------------------
  // Step 3: playing backwards
  // ---------------------------------------------------------------------------------

  /**
   * Undo pushes at random. Many random choices do not work out, so this keeps
   * trying until
   * enough pulls have been made or it runs out of attempts.
   */
  private List<Pull> undoPushes(int pullCount) {
    List<Pull> pulls = new ArrayList<>();
    int attempts = 0;
    while (pulls.size() < pullCount && attempts < pullCount * ATTEMPTS_PER_PULL) {
      attempts++;
      Pull pull = tryToUndoOnePush();
      if (pull != null) {
        pulls.add(pull);
      }
    }
    return pulls;
  }

  /**
   * Pick a random box and a random direction, and undo a push of that box if it
   * is possible.
   * Picture the push that is being undone: a player stood at {@code behind},
   * stepped into
   * {@code before}, and shoved the box from {@code before} to where it is now. To
   * undo it
   * the player walks to {@code before} and then steps back to {@code behind},
   * dragging the
   * box with them.
   *
   * @return what was done, or null if this choice was not possible
   */
  private Pull tryToUndoOnePush() {
    int boxNumber = random.nextInt(boxes.size());
    Direction pushDirection = Direction.values()[random.nextInt(Direction.values().length)];

    Square box = boxes.get(boxNumber);
    Square before = box.next(pushDirection.opposite());
    Square behind = before.next(pushDirection.opposite());
    if (!isFree(before) || !isFree(behind)) {
      return null;
    }
    List<Direction> walk = findPath(player, before);
    if (walk == null) {
      return null; // The player cannot get to that square.
    }

    boxes.set(boxNumber, before); // The box moves back one square...
    player = behind; // ...and the player ends up behind where it was.
    return new Pull(walk, pushDirection);
  }

  // ---------------------------------------------------------------------------------
  // Walking
  // ---------------------------------------------------------------------------------

  /** True if this square is a wall, or is off the board. */
  private boolean isWall(Square square) {
    boolean offBoard = square.row() < 0 || square.row() >= wall.length
        || square.column() < 0 || square.column() >= wall[0].length;
    return offBoard || wall[square.row()][square.column()];
  }

  private boolean hasBox(Square square) {
    return boxes.contains(square);
  }

  /** True if the player could step onto this square: no wall and no box. */
  private boolean isFree(Square square) {
    return !isWall(square) && !hasBox(square);
  }

  /**
   * Find the shortest walk between two squares with a breadth-first search: look
   * at every
   * square one step away, then every square two steps away, and so on, so the
   * first time we
   * reach the target we got there in as few steps as possible.
   *
   * @return the steps to take (empty if already there), or null if there is no
   *         way
   */
  private List<Direction> findPath(Square from, Square to) {
    boolean[][] visited = new boolean[wall.length][wall[0].length];
    // For each visited square: the step we took to arrive there.
    Direction[][] arrivedBy = new Direction[wall.length][wall[0].length];

    ArrayDeque<Square> queue = new ArrayDeque<>();
    queue.add(from);
    visited[from.row()][from.column()] = true;

    while (!queue.isEmpty()) {
      Square current = queue.remove();
      if (current.equals(to)) {
        return rebuildPath(arrivedBy, from, to);
      }
      for (Direction direction : Direction.values()) {
        Square next = current.next(direction);
        if (isFree(next) && !visited[next.row()][next.column()]) {
          visited[next.row()][next.column()] = true;
          arrivedBy[next.row()][next.column()] = direction;
          queue.add(next);
        }
      }
    }
    return null;
  }

  /**
   * Follow the recorded steps backwards from the target to the start, then flip
   * them round.
   */
  private List<Direction> rebuildPath(Direction[][] arrivedBy, Square from, Square to) {
    List<Direction> path = new ArrayList<>();
    Square current = to;
    while (!current.equals(from)) {
      Direction step = arrivedBy[current.row()][current.column()];
      path.add(step);
      current = current.next(step.opposite()); // Go back to where that step began.
    }
    Collections.reverse(path);
    return path;
  }

  // ---------------------------------------------------------------------------------
  // Writing the result
  // ---------------------------------------------------------------------------------

  /** How many boxes are standing somewhere other than on a goal. */
  private int countBoxesOffGoals() {
    int count = 0;
    for (Square box : boxes) {
      if (!goals.contains(box)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Turn the board into rows of level symbols, as they are found in a level file.
   */
  private List<String> buildRows() {
    List<String> rows = new ArrayList<>();
    for (int row = 0; row < wall.length; row++) {
      StringBuilder text = new StringBuilder();
      for (int column = 0; column < wall[row].length; column++) {
        text.append(tileAt(new Square(row, column)));
      }
      rows.add(text.toString());
    }
    return rows;
  }

  private char tileAt(Square square) {
    if (isWall(square)) {
      return Tiles.WALL;
    }
    boolean onGoal = goals.contains(square);
    if (hasBox(square)) {
      return onGoal ? Tiles.BOX_ON_GOAL : Tiles.BOX;
    }
    if (square.equals(player)) {
      return onGoal ? Tiles.PLAYER_ON_GOAL : Tiles.PLAYER;
    }
    return onGoal ? Tiles.GOAL : Tiles.FLOOR;
  }

  /**
   * The moves that solve the level: undo the undone pushes, last one first. After
   * each
   * push the player walks back to where they stood before that pull began, which
   * is
   * exactly where the next push (the one before it in time) starts from.
   */
  private List<Direction> buildSolution(List<Pull> pulls) {
    List<Direction> solution = new ArrayList<>();
    for (int index = pulls.size() - 1; index >= 0; index--) {
      Pull pull = pulls.get(index);
      solution.add(pull.pushDirection());
      boolean notTheLastPush = index > 0;
      if (notTheLastPush) {
        solution.addAll(walkBackwards(pull.walk()));
      }
    }
    return solution;
  }

  /**
   * The same walk done in reverse: the steps in the opposite order and the
   * opposite way.
   */
  private static List<Direction> walkBackwards(List<Direction> walk) {
    List<Direction> back = new ArrayList<>();
    for (int index = walk.size() - 1; index >= 0; index--) {
      back.add(walk.get(index).opposite());
    }
    return back;
  }
}
