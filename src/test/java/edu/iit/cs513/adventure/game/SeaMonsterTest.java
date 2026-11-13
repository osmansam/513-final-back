package edu.iit.cs513.adventure.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SeaMonsterTest {
    @Test
    void patrolsInsideItsAreaAndTurnsAtTheBoundary() {
        SeaMonster monster = new SeaMonster(new Position(1, 1), new OceanMap(5, 5, Set.of()),
                new RoamingArea(1, 1, 3, 3));
        for (Position expected : List.of(new Position(2, 1), new Position(3, 1),
                new Position(3, 2), new Position(3, 3), new Position(2, 3),
                new Position(1, 3), new Position(1, 2), new Position(1, 1))) {
            monster.move();
            assertEquals(expected, monster.getPosition());
        }
    }

    @Test
    void turnsAroundAnIslandInsteadOfCrossingIt() {
        SeaMonster monster = new SeaMonster(new Position(1, 1),
                new OceanMap(5, 5, Set.of(new Position(2, 1))), new RoamingArea(1, 1, 3, 3));

        monster.move();

        assertEquals(new Position(1, 2), monster.getPosition());
    }

    @Test
    void trappedMonsterStaysPutWithoutLeavingItsRegion() {
        OceanMap map = new OceanMap(3, 3, Set.of(new Position(1, 0), new Position(2, 1),
                new Position(1, 2), new Position(0, 1)));
        SeaMonster monster = new SeaMonster(new Position(1, 1), map, new RoamingArea(0, 0, 2, 2));

        monster.move();

        assertEquals(new Position(1, 1), monster.getPosition());
    }

    @Test
    void rejectsReversedOrNegativeRoamingBounds() {
        assertThrows(IllegalArgumentException.class, () -> new RoamingArea(3, 1, 1, 3));
        assertThrows(IllegalArgumentException.class, () -> new RoamingArea(1, 3, 3, 1));
        assertThrows(IllegalArgumentException.class, () -> new RoamingArea(-1, 0, 2, 2));
        assertThrows(IllegalArgumentException.class, () -> new RoamingArea(0, -1, 2, 2));
    }

    @Test
    void rejectsInvalidStartingCellsOrRegionsOutsideTheMap() {
        OceanMap map = new OceanMap(4, 4, Set.of(new Position(2, 2)));
        assertThrows(IllegalArgumentException.class,
                () -> new SeaMonster(new Position(2, 2), map, new RoamingArea(0, 0, 3, 3)));
        assertThrows(IllegalArgumentException.class,
                () -> new SeaMonster(new Position(0, 0), map, new RoamingArea(1, 1, 3, 3)));
        assertThrows(IllegalArgumentException.class,
                () -> new SeaMonster(new Position(1, 1), map, new RoamingArea(0, 0, 4, 3)));
    }
}
