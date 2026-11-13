package edu.iit.cs513.adventure.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@SuppressWarnings("deprecation")
class ShipTest {
    @ParameterizedTest
    @CsvSource({"NORTH,2,1", "EAST,3,2", "SOUTH,2,3", "WEST,1,2"})
    void movesOneCellInRequestedDirection(Direction direction, int x, int y) {
        Ship ship = new Ship(new Position(2, 2), new OceanMap(5, 5, Set.of()));

        assertTrue(ship.move(direction));
        assertEquals(new Position(x, y), ship.getPosition());
    }

    @ParameterizedTest
    @CsvSource({"NORTH,0,0", "WEST,0,0", "SOUTH,2,2", "EAST,2,2"})
    void cannotLeaveAnyEdge(Direction direction, int x, int y) {
        Position start = new Position(x, y);
        Ship ship = new Ship(start, new OceanMap(3, 3, Set.of()));

        assertFalse(ship.move(direction));
        assertEquals(start, ship.getPosition());
    }

    @Test
    void blockedMoveDoesNotNotifyObservers() {
        Ship ship = new Ship(new Position(0, 0),
                new OceanMap(3, 3, Set.of(new Position(1, 0))));
        List<Object> notifications = new ArrayList<>();
        ship.addObserver((source, position) -> notifications.add(position));

        assertFalse(ship.move(Direction.EAST));
        assertFalse(ship.move(Direction.NORTH));
        assertEquals(new Position(0, 0), ship.getPosition());
        assertTrue(notifications.isEmpty());
    }

    @Test
    void successfulMovesPublishTheirNewImmutableCoordinates() {
        Ship ship = new Ship(new Position(0, 0), new OceanMap(3, 3, Set.of()));
        List<Object> notifications = new ArrayList<>();
        ship.addObserver((source, position) -> notifications.add(position));

        ship.move(Direction.EAST);
        ship.move(Direction.SOUTH);

        assertEquals(List.of(new Position(1, 0), new Position(1, 1)), notifications);
    }

    @ParameterizedTest
    @CsvSource({"-1,0", "3,0", "1,1"})
    void rejectsStartingOutsideWater(int x, int y) {
        OceanMap map = new OceanMap(3, 3, Set.of(new Position(1, 1)));
        assertThrows(IllegalArgumentException.class, () -> new Ship(new Position(x, y), map));
    }
}
