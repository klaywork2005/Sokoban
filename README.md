# Sokoban

A Java 21 / Swing project. Load a level, move with WASD or the arrow keys,
and push boxes onto goals. Undo and solving are future features.
Click **Go To Level** to load a selected level or restart the current one.

## Run and test

Install a JDK 21 or newer and Maven. A JDK includes the compiler; a JRE only runs
programs. Check `java -version` and `mvn -version`: Maven must use JDK 21 or newer.
In VS Code, select that JDK using **Java: Configure Java Runtime** as well.

If the terminal reports Java `1.8`, it is using Java 8. Set `JAVA_HOME` to your
JDK installation folder, put `%JAVA_HOME%\bin` before older Java entries in your
Windows `Path`, and open a new terminal. If `mvn` is not recognized, add your
Maven installation's `bin` folder to `Path`. Setting the IDE's runtime alone
does not necessarily change which Java the terminal uses.

From the project folder:

```sh
mvn compile exec:java
mvn test
```

You can also run `sokoban.Main` from your IDE after importing the Maven project.
Level files come from the classpath, so launching from another directory works.
To view Microban instead, change `LEVEL_RESOURCE` in `Main` to `/microban.txt`.

## Warehouse artwork

The game uses pixel sprites in `src/main/resources/sokoban/tiles/`: dark red-brown
brick walls, sandy wood planks, outdoor cement, a worker, and wooden crates.
Crates turn redwood when placed on goals. Unfilled goals are small white dots;
a player standing on a goal keeps the dot visible at their feet.

`WarehouseSprites` loads the PNGs from the classpath once and samples them onto
a 48-pixel tile grid. `LevelRenderer` uses nearest-neighbor scaling so resized
boards stay sharp. `WarehouseLayout` identifies exterior spaces from the saved
level, including short rows, while keeping enclosed rooms wooden. Wall borders
are drawn only on exposed edges.

Artwork was created with built-in imagegen. Exact generation and edit prompts
are saved alongside the PNGs in `asset-prompts-terrain.txt` and
`asset-prompts-entities.txt`.

## Where code belongs

| Class | Responsibility |
| --- | --- |
| `Main` | Load resources, create the shared objects, and start Swing. |
| `LevelLoader` | Read level files. Preserve spaces because they position tiles. |
| `State` | Own the saved levels, selected index, and changing board. No UI or file access. |
| `GameLogic` | Perform game actions using the shared state. No Swing code. |
| `GameWindow` | Create controls, handle input, call game actions, refresh the view. |
| `LevelRenderer` | Draw the current board. Never change game data while painting. |

There is **one State object**, created in `Main`. The game, window, and renderer
all receive that same object. Do not create another State to read the current
level: that would create an independent game. Pass the existing instance into
new classes that need it.

Normal fields belong to an object; `static` fields belong to a class. Use
`private static final` for fixed constants such as tile size. Store changing
game data in State instead of public static fields. `private` fields protect
data; methods provide the allowed ways to read or update it. `final` prevents
reassigning a field, but does not by itself make an object immutable.

## Follow an action through the code

1. The player clicks **Go To Level** in `GameWindow`.
2. The listener calls `game.selectLevel(index)`.
3. `GameLogic` updates the shared `State`, which checks the index.
4. `GameWindow.refreshView()` updates the controls and requests a repaint.
5. Swing calls `LevelRenderer.paintComponent()`, which reads that same State.

Changing state does not automatically repaint Swing. Follow an action with
`refreshView()` in the window. Swing UI actions and state updates currently run
on Swing's event thread. Keep slow work such as a future solver off that thread;
use a `SwingWorker` and apply the result on the event thread.

The dropdown is a pending selection until the button is clicked. The status line
shows the level actually being displayed. Java indices start at 0; displayed
level numbers start at 1.

## How movement works

`GameWindow` binds WASD and arrow keys to `game.moveCharacter(dr, dc)`.
`dr` changes the row and `dc` changes the column. Up is `(-1, 0)` and
right is `(0, 1)`.

`State` keeps a `char[][]` board copied from the saved level. `GameLogic`
checks the next square before moving. If it contains a box, the square beyond
must be empty floor or a goal. Walls, another box, and board edges block a push.
The renderer reads the changing board after each action.

The characters are `#` for walls, a space for floor, `.` for goals, `$` for
boxes, and `@` for the player. `*` means a box on a goal and `+` means the
player on a goal. Movement restores the goal when its occupant leaves.

The saved rows returned by `getCurrentLevel()` stay unchanged. Selecting a
level copies those rows into a fresh board and finds the player's position.

The original development checklist is in `.instructions.txt`; it describes
planned features rather than features already implemented.
