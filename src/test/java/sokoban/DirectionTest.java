package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Check the small helpers on the direction enum. */
class DirectionTest {
    @Test
    void oppositeGivesTheDirectionThatUndoesAMove() {
        assertEquals(Direction.DOWN, Direction.UP.opposite());
        assertEquals(Direction.UP, Direction.DOWN.opposite());
        assertEquals(Direction.RIGHT, Direction.LEFT.opposite());
        assertEquals(Direction.LEFT, Direction.RIGHT.opposite());
    }

    @Test
    void takingAStepAndThenItsOppositeReturnsToTheStart() {
        for (Direction direction : Direction.values()) {
            Direction back = direction.opposite();
            assertEquals(0, direction.dr() + back.dr());
            assertEquals(0, direction.dc() + back.dc());
        }
    }
}
