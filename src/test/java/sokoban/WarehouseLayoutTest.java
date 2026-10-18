package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Check how static geometry separates outdoor padding from enclosed warehouse floor. */
class WarehouseLayoutTest {
    @Test
    void separatesJaggedPaddingAndLeadingSpacesFromEnclosedFloor() {
        // The map has leading spaces and a final row shorter than its bounding rectangle.
        WarehouseLayout layout = new WarehouseLayout(List.of(
                "  #####",
                "  #@ .#",
                "###   #",
                "# $ ###",
                "#####"));

        // Padding is outdoor concrete; actual enclosed spaces remain indoor flooring.
        assertEquals(5, layout.getRowCount());
        assertEquals(7, layout.getColumnCount());
        assertTrue(layout.isExterior(1, 1));
        assertTrue(layout.isExterior(4, 5));
        assertTrue(layout.isExterior(4, 6));
        assertFalse(layout.isWall(4, 6));
        assertFalse(layout.isExterior(1, 4));
        assertFalse(layout.isExterior(2, 5));
        assertFalse(layout.isExterior(3, 1));
        assertTrue(layout.isWall(2, 0));
        assertFalse(layout.isExterior(2, 0));
    }

    @Test
    void preservesSealedEmptyRoomsEvenWhenThePlayerCannotReachThem() {
        // The right-hand room is sealed away from both the player and the map boundary.
        WarehouseLayout layout = new WarehouseLayout(List.of(
                "#########",
                "#@ #    #",
                "#########"));

        assertFalse(layout.isExterior(1, 2));
        // Interior classification depends on the outside flood, not player reachability.
        for (int column = 4; column < 8; column++) {
            assertFalse(layout.isExterior(1, column));
        }
    }

    @Test
    void floodReachesSpacesThroughAConcaveExteriorOpening() {
        // An opening on the left connects to a bent outdoor passage around a wall.
        WarehouseLayout layout = new WarehouseLayout(List.of(
                "#######",
                "#@#   #",
                "### # #",
                "    # #",
                "#######"));

        assertTrue(layout.isExterior(3, 0));
        assertTrue(layout.isExterior(1, 3));
        assertTrue(layout.isExterior(3, 5));
        assertFalse(layout.isExterior(1, 1));
    }

    @Test
    void floodCannotCrossWallsDiagonallyOrPassThroughObjects() {
        // Corner contact alone must not let the flood reach the enclosed blank square.
        WarehouseLayout diagonal = new WarehouseLayout(List.of(
                " ###",
                "# ##",
                "####"));
        assertTrue(diagonal.isExterior(0, 0));
        assertFalse(diagonal.isExterior(1, 1));

        // Every non-space object blocks the flood, even when placed on the boundary.
        for (char tile : new char[] {'@', '$', '.', '*', '+'}) {
            WarehouseLayout blocked = new WarehouseLayout(List.of(
                    "#####",
                    tile + "   #",
                    "#####"));
            assertFalse(blocked.isExterior(1, 0));
            assertFalse(blocked.isExterior(1, 1));
        }
    }

    @Test
    void treatsCoordinatesBeyondTheRectangleAsOutsideWithoutWalls() {
        WarehouseLayout layout = new WarehouseLayout(List.of("#"));
        // Check one coordinate past each side of a one-tile map.
        for (int[] point : new int[][] {{-1, 0}, {0, -1}, {1, 0}, {0, 1}}) {
            assertTrue(layout.isExterior(point[0], point[1]));
            assertFalse(layout.isWall(point[0], point[1]));
        }

        // Empty inputs must answer safely without indexing zero-sized masks.
        WarehouseLayout empty = new WarehouseLayout(List.of());
        assertEquals(0, empty.getRowCount());
        assertEquals(0, empty.getColumnCount());
        assertTrue(empty.isExterior(0, 0));
        assertFalse(empty.isWall(0, 0));

        WarehouseLayout emptyRow = new WarehouseLayout(List.of(""));
        assertEquals(1, emptyRow.getRowCount());
        assertEquals(0, emptyRow.getColumnCount());
        assertTrue(emptyRow.isExterior(0, 0));
    }
}
