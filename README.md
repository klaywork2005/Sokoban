# Sokoban

A Sokoban puzzle game written in Java 21 with Swing. Push every box onto a goal
and keep your move count low.

## Features

- Two bundled level collections: the 51-level **Original** set and the 155-level **Microban** set.
- Keyboard play with WASD or the arrow keys.
- Scoring: every attempt starts at 10,000 points and each successful move costs 50.
- Personal records saved between sessions: best score and fewest moves per level.
- A **level generator** that makes new levels on demand, each one guaranteed to be solvable.
- Looping background music.
- Pixel-art warehouse artwork that scales to the window.

## Requirements

- A **JDK 21 or newer** (a JRE can run programs but cannot compile them).
- [Maven](https://maven.apache.org/), running on that JDK.

Check your setup with `java -version` and `mvn -version`.

**If the terminal reports Java 1.8**, it is using Java 8. Set `JAVA_HOME` to your JDK folder,
put `%JAVA_HOME%\bin` before older Java entries in your Windows `Path`, and open a new terminal.
If `mvn` is not recognized, add Maven's `bin` folder to `Path`. Selecting a runtime inside your
IDE (for example **Java: Configure Java Runtime** in VS Code) does not change which Java the
terminal uses.

## Run and test

From the project folder:

```sh
mvn compile exec:java   # start the game
mvn test                # run the unit tests
```

You can also run `sokoban.Main` from your IDE after importing the Maven project. Level files
are loaded from the classpath, so the program works from any working directory.

To play Microban instead of the Original levels, change `LEVEL_RESOURCE` in
[Main.java](src/main/java/sokoban/Main.java) to `"/microban.txt"`.

## How to play

| Action | Control |
| --- | --- |
| Move / push a box | `W` `A` `S` `D` or the arrow keys |
| Load the level chosen in the dropdown | **Go To Level** |
| Restart the loaded level | **Reset Level** |
| Make a brand new level and play it | **Generate Level** |

Push boxes onto goals (`.`). Boxes can be pushed but never pulled, and only one box can be
pushed at a time. When every box is on a goal the attempt ends; reset or pick another level
to continue. Blocked moves cost nothing, and the score never drops below zero.

The status bar at the bottom shows the current level, moves, score, and your best score and
fewest moves for that level. Completed levels are marked in the dropdown.

### Generated levels

**Generate Level** builds a new level and puts it at the end of the level dropdown as
"Generated level". Clicking the button again replaces it. Generated levels do not save records,
because they are not part of the level file.

### Personal records

Records are loaded at startup and saved after every completion to
`%USERPROFILE%\.sokoban\progress.properties` (the `.sokoban` folder in your home directory on
other systems). Replaying a level keeps the highest score and lowest move count.
Each level collection has its own records. Only one local player is tracked, and unfinished
boards are not saved.

A damaged save file is reported at startup and left untouched. Level IDs are built from the
resource name and level number (for example `/original-levels.txt:3`), so reordering the levels
in a collection would require migrating its records.

## Project layout

```text
src/main/java/sokoban/          Game, window, and rendering
src/main/resources/             Level files (*.txt), sprite PNGs (sokoban/tiles/), and the music (*.mp3)
src/test/java/                  JUnit 5 tests, mirroring the main packages
```

### Package `sokoban`

| Class | Responsibility |
| --- | --- |
| `Main` | Loads the levels and records, creates the shared objects, and opens the window. |
| `LevelLoader` | Reads level files. Spaces are preserved because they position tiles. |
| `Tiles` | Names for the level symbols (`#`, `$`, `@`, ...) and small helpers such as `isBox`. |
| `Direction` | The four moves (`UP`, `DOWN`, `LEFT`, `RIGHT`) as row/column offsets. |
| `BoardRules` | The rules of Sokoban as pure functions: moving, pushing, and detecting a win. |
| `State` | The shared game data: saved levels, selected level, the board being played, and the move count. No UI or file access. |
| `GameLogic` | Applies player actions to the `State`, tracks completion, and records results. No Swing code. |
| `PlayerProgress` / `PlayerRecord` | The player's best score and fewest moves per completed level, in memory. |
| `ProgressStore` | Saves and loads `PlayerProgress` as a properties file. |
| `GameWindow` | Builds the controls, handles input, calls game actions, and refreshes the display. |
| `LevelRenderer` | Draws the current board. It only reads game data. |
| `WarehouseLayout` | Works out which squares are walls and which are outdoors, from the saved level. |
| `WarehouseSprites` | Loads the PNG artwork once and scales it to one tile size. |
| `LevelGenerator` | Makes new solvable levels by playing a finished level backwards. No Swing code. |
| `BackgroundMusic` | Loops `Soboku_Music.mp3` quietly on a background thread, using the JLayer library to decode the MP3. |

## Architecture notes

There is **one `State` object**, created in `Main`. The game logic, window, and renderer all
receive that same instance. Do not create another `State` to read the current level: that would
create an independent game. Pass the existing instance into any new class that needs it.

Changing the state does not repaint the screen by itself. After a game action,
`GameWindow.refreshView()` updates the labels and asks Swing to repaint.
All Swing components and game-state changes run on Swing's event thread.

Fixed constants use `private static final`. Changing game data lives in `State`, never in
public static fields.

### Following a click through the code

1. The player clicks **Go To Level** in `GameWindow`.
2. The listener calls `game.selectLevel(index)`.
3. `GameLogic` updates the shared `State`, which validates the index.
4. `GameWindow.refreshView()` updates the controls and requests a repaint.
5. Swing calls `LevelRenderer.paintComponent()`, which reads that same `State`.

The dropdown is a pending choice until the button is clicked; the status line always shows the
level actually on screen. Java indices start at 0; displayed level numbers start at 1.

### How movement works

`GameWindow` binds each key to a `Direction`. `GameLogic.moveCharacter(dr, dc)` takes a row
change `dr` and a column change `dc`: up is `(-1, 0)` and right is `(0, 1)`.

`BoardRules` checks the next square before anything moves. If it holds a box, the square beyond
must be bare floor or a goal. Walls, other boxes, and the board edge block a push. A legal move
returns a new board, which `State` then adopts.

### Level format

Level files use the common XSB format. Consecutive lines of the symbols below make one level; any
other line (blank, title, comment) ends the level being read.

| Symbol | Meaning |
| --- | --- |
| `#` | wall |
| (space) | floor |
| `.` | goal |
| `$` | box |
| `@` | player |
| `*` | box on a goal |
| `+` | player on a goal |

Moving restores the goal underneath when a box or the player leaves it. The saved rows returned by
`State.getCurrentLevel()` never change; selecting a level copies them into a fresh board.

## How the level generator works

Making a random level and then checking whether it can be solved is hard. `LevelGenerator` avoids
the problem by building a level **backwards**, so it is solvable by construction:

1. **Build a room.** Draw an outer wall, then try to add some random walls inside. A wall is
   taken back if it would split the room in two (a breadth-first search checks this).
2. **Finish the level.** Put every box on its own goal, and the player on any free square.
3. **Undo pushes at random.** Pick a box and a direction. If the player can walk to the square
   next to the box, the box and player each step back one square, dragging the box away from its
   goal. This is exactly a Sokoban push played in reverse. Repeat many times; the more pulls, the
   more mixed up the boxes get. Pulls that are not possible (a wall is in the way, say) are skipped.
4. **Reject boring levels.** If most boxes ended up back on their goals, or the solution would be
   very short, start over.

Because every pull can be undone by a normal push, playing the pulls in reverse order solves the
level. `LevelGenerator` returns those solving moves along with the level. The game never shows
them, but `LevelGeneratorTest` replays them on the real game rules for hundreds of different
levels to prove each one can be finished.

## Artwork and credits

The game uses pixel sprites in `src/main/resources/sokoban/tiles/`: dark red-brown brick walls,
sandy wood planks, outdoor cement, a worker, and wooden crates. Crates turn redwood on goals.
Unfilled goals are small white dots; a player standing on a goal keeps the dot visible at their
feet. The artwork was generated with an image-generation tool, and the exact prompts are saved next
to the PNGs in `asset-prompts-terrain.txt` and `asset-prompts-entities.txt`.

`WarehouseSprites` scales every image onto a 48-pixel tile grid, and `LevelRenderer` uses
nearest-neighbor scaling so resized boards stay sharp.

The background music is `src/main/resources/Soboku_Music.mp3`. The song is mastered very loud, so
`BackgroundMusic` plays it at 25% volume (`VOLUME`); change that one constant to make it louder or
quieter. If the sound device or file is unavailable, the game runs silently.

Level collections:

- **Original**: the original Sokoban levels by Thinking Rabbit (1988), as collected at
  [Sokoban. The beautiful world of remodels](http://sokoban-jd.blogspot.com/).
- **Microban**: by David W. Skinner.

## Roadmap

The original development checklist is kept in [.instructions.txt](.instructions.txt). Open items:

- Undo.
- An optional runnable JAR.
