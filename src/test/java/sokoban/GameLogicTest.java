package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Exercise movement rules directly against tiny boards, without opening Swing. */
class GameLogicTest {
    // Each fixture uses XSB symbols; assertions read the mutable board after actions.
    @Test
    void playerCanWalkInAllFourDirections() {
        // Put the player in open floor so a four-step loop is unobstructed.
        State state = new State(List.of(List.of("#####", "#   #", "# @ #", "#   #", "#####")));
        GameLogic game = new GameLogic(state);

        // Check each direction and confirm the last vacated square becomes floor.
        game.moveCharacter(-1, 0);
        assertEquals('@', state.getTile(1, 2));
        game.moveCharacter(0, -1);
        assertEquals('@', state.getTile(1, 1));
        game.moveCharacter(1, 0);
        assertEquals('@', state.getTile(2, 1));
        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(2, 2));
        assertEquals(' ', state.getTile(2, 1));
    }

    @Test
    void playerCannotWalkThroughWallsOrMoveDiagonally() {
        State state = new State(List.of(List.of("#####", "#@  #", "#####")));
        GameLogic game = new GameLogic(state);

        // Try a wall, a diagonal, a two-square jump, and a zero-length move.
        game.moveCharacter(-1, 0);
        game.moveCharacter(1, 1);
        game.moveCharacter(0, 2);
        game.moveCharacter(0, 0);

        // All rejected moves must leave the original player and floor untouched.
        assertEquals('@', state.getTile(1, 1));
        assertEquals(' ', state.getTile(1, 2));
    }

    @Test
    void boxMovesOntoAndOffGoalWithoutErasingIt() {
        State state = new State(List.of(List.of("#######", "#@$.  #", "#######")));
        GameLogic game = new GameLogic(state);

        // First push covers the goal with a box (*).
        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(1, 2));
        assertEquals('*', state.getTile(1, 3));

        // Second push leaves the player on the exposed goal (+).
        game.moveCharacter(0, 1);
        assertEquals('+', state.getTile(1, 3));
        assertEquals('$', state.getTile(1, 4));

        // Leaving the goal restores its dot, while the saved template stays unchanged.
        game.moveCharacter(0, -1);
        assertEquals('.', state.getTile(1, 3));
        assertEquals('@', state.getTile(1, 2));
        assertEquals("#@$.  #", state.getCurrentLevel().get(1));
    }

    @Test
    void boxCannotBePushedIntoWallOrAnotherBox() {
        // Separate levels exercise a wall behind the box and a second box behind it.
        State state = new State(List.of(List.of("#@$#"), List.of("#@$$ #")));
        GameLogic game = new GameLogic(state);

        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(0, 1));
        assertEquals('$', state.getTile(0, 2));

        game.selectLevel(1);
        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(0, 1));
        assertEquals('$', state.getTile(0, 2));
        assertEquals('$', state.getTile(0, 3));
    }

    @Test
    void boxCanBePushedUpDownAndLeft() {
        // Rightward pushes are covered above; these fixtures cover the other directions.
        State state = new State(List.of(
                List.of("#####", "#   #", "# $ #", "# @ #", "#####"),
                List.of("#####", "# @ #", "# $ #", "#   #", "#####"),
                List.of("######", "# $@ #", "######")));
        GameLogic game = new GameLogic(state);

        game.moveCharacter(-1, 0);
        assertEquals('$', state.getTile(1, 2));
        assertEquals('@', state.getTile(2, 2));

        game.selectLevel(1);
        game.moveCharacter(1, 0);
        assertEquals('$', state.getTile(3, 2));
        assertEquals('@', state.getTile(2, 2));

        game.selectLevel(2);
        game.moveCharacter(0, -1);
        assertEquals('$', state.getTile(1, 1));
        assertEquals('@', state.getTile(1, 2));
    }

    @Test
    void playerCanStartOnGoalAndLeaveIt() {
        // A starting + must behave like a player while preserving the goal below.
        State state = new State(List.of(List.of("#+ #")));
        GameLogic game = new GameLogic(state);

        game.moveCharacter(0, 1);
        assertEquals('.', state.getTile(0, 1));
        assertEquals('@', state.getTile(0, 2));
        game.moveCharacter(0, -1);
        assertEquals('+', state.getTile(0, 1));
    }

    @Test
    void missingSquaresBlockWalkingAndPushing() {
        // Include a short row, an empty row, and a box at the right-hand edge.
        State state = new State(List.of(List.of("@ ", ""), List.of("@$")));
        GameLogic game = new GameLogic(state);

        // Missing neighbors block movement, but a real floor square remains walkable.
        game.moveCharacter(-1, 0);
        game.moveCharacter(0, -1);
        game.moveCharacter(1, 0);
        assertEquals('@', state.getTile(0, 0));
        game.moveCharacter(0, 1);
        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(0, 1));

        // The box cannot be pushed beyond the end of its row.
        game.selectLevel(1);
        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(0, 0));
        assertEquals('$', state.getTile(0, 1));
    }

    @Test
    void selectingLevelResetsBoardAndPlayerPosition() {
        State state = new State(List.of(List.of("#@  #"), List.of("#  @ #")));
        GameLogic game = new GameLogic(state);

        // Selecting the current level must restart it from the saved player position.
        game.moveCharacter(0, 1);
        game.selectLevel(0);
        assertEquals('@', state.getTile(0, 1));
        assertEquals(' ', state.getTile(0, 2));

        // Switching layouts must also refresh GameLogic's cached player coordinates.
        game.selectLevel(1);
        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(0, 4));

        game.selectLevel(0);
        game.moveCharacter(0, 1);
        assertEquals('@', state.getTile(0, 2));
    }
}
