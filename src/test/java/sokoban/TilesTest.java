package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Check that each tile symbol is classified correctly, including the combined ones. */
class TilesTest {
    @Test
    void everySymbolOfTheLevelFormatIsValid() {
        for (char tile : " #.$@*+".toCharArray()) {
            assertTrue(Tiles.isValid(tile), "Expected '" + tile + "' to be valid.");
        }
    }

    @Test
    void otherCharactersAreNotValid() {
        for (char tile : "abcXY019-_;:\t".toCharArray()) {
            assertFalse(Tiles.isValid(tile), "Expected '" + tile + "' to be invalid.");
        }
    }

    @Test
    void boxesAreRecognizedWithAndWithoutAGoalUnderneath() {
        assertTrue(Tiles.isBox(Tiles.BOX));
        assertTrue(Tiles.isBox(Tiles.BOX_ON_GOAL));
        assertFalse(Tiles.isBox(Tiles.PLAYER));
        assertFalse(Tiles.isBox(Tiles.GOAL));
    }

    @Test
    void playersAreRecognizedWithAndWithoutAGoalUnderneath() {
        assertTrue(Tiles.isPlayer(Tiles.PLAYER));
        assertTrue(Tiles.isPlayer(Tiles.PLAYER_ON_GOAL));
        assertFalse(Tiles.isPlayer(Tiles.BOX));
        assertFalse(Tiles.isPlayer(Tiles.FLOOR));
    }

    @Test
    void goalsAreRecognizedEvenWhenSomethingCoversThem() {
        assertTrue(Tiles.isGoal(Tiles.GOAL));
        assertTrue(Tiles.isGoal(Tiles.BOX_ON_GOAL));
        assertTrue(Tiles.isGoal(Tiles.PLAYER_ON_GOAL));
        assertFalse(Tiles.isGoal(Tiles.FLOOR));
        assertFalse(Tiles.isGoal(Tiles.BOX));
        assertFalse(Tiles.isGoal(Tiles.PLAYER));
    }

    @Test
    void openFloorIsOnlyEmptyFloorAndEmptyGoals() {
        assertTrue(Tiles.isOpenFloor(Tiles.FLOOR));
        assertTrue(Tiles.isOpenFloor(Tiles.GOAL));
        for (char tile : "#$@*+".toCharArray()) {
            assertFalse(Tiles.isOpenFloor(tile), "Expected '" + tile + "' to be occupied.");
        }
    }

    @Test
    void insideRoomMeansAnyValidTileExceptWalls() {
        for (char tile : " .$@*+".toCharArray()) {
            assertTrue(Tiles.isInsideRoom(tile), "Expected '" + tile + "' inside the room.");
        }
        assertFalse(Tiles.isInsideRoom(Tiles.WALL));
        assertFalse(Tiles.isInsideRoom('x'));
    }
}
