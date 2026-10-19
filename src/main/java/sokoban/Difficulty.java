package sokoban;

/**
 * How hard a generated level should be. Each setting is a ready-made recipe for
 * {@link LevelGenerator#generate(int, int, int, int, int)}: bigger rooms, more boxes and more
 * undone pushes make a harder puzzle.
 */
public enum Difficulty {
    //    label     width height boxes walls pulls
    EASY("Easy",     7,    7,     2,    4,   15),
    MEDIUM("Medium", 9,    9,     3,    8,   30),
    HARD("Hard",    11,   10,     5,   12,   60);

    private final String label;
    private final int width;
    private final int height;
    private final int boxes;
    private final int walls;
    private final int pulls;

    Difficulty(String label, int width, int height, int boxes, int walls, int pulls) {
        this.label = label;
        this.width = width;
        this.height = height;
        this.boxes = boxes;
        this.walls = walls;
        this.pulls = pulls;
    }

    /** Columns in the room, including the outer wall. */
    public int width() {
        return width;
    }

    /** Rows in the room, including the outer wall. */
    public int height() {
        return height;
    }

    /** How many boxes (and goals) the level has. */
    public int boxes() {
        return boxes;
    }

    /** How many extra walls to try to place inside the room. */
    public int walls() {
        return walls;
    }

    /** How many pushes the generator undoes. More pulls mix the boxes up further. */
    public int pulls() {
        return pulls;
    }

    /** The name shown to the player. A drop-down list shows whatever toString() returns. */
    @Override
    public String toString() {
        return label;
    }
}
