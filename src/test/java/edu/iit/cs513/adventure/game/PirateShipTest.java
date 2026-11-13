package edu.iit.cs513.adventure.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@SuppressWarnings("deprecation")
class PirateShipTest {
    @ParameterizedTest
    @CsvSource({"5,0,4,0", "0,5,0,4", "5,5,4,5"})
    void chasesOnTheLargerDistanceAxisWithHorizontalTieBreak(int x, int y, int expectedX, int expectedY) {
        OceanMap map = new OceanMap(6, 6, Set.of());
        Ship player = new Ship(new Position(1, 0), map);
        PirateShip pirate = new PirateShip(new Position(x, y), map);
        player.addObserver(pirate);

        player.move(Direction.SOUTH);

        assertEquals(new Position(expectedX, expectedY), pirate.getPosition());
    }

    @Test
    void fallsBackToVerticalWhenHorizontalStepIsBlocked() {
        OceanMap map = new OceanMap(6, 6, Set.of(new Position(4, 0)));
        Ship player = new Ship(new Position(1, 1), map);
        PirateShip pirate = new PirateShip(new Position(5, 0), map);
        player.addObserver(pirate);

        player.move(Direction.SOUTH);

        assertEquals(new Position(5, 1), pirate.getPosition());
    }

    @Test
    void fallsBackToHorizontalWhenVerticalStepIsBlocked() {
        OceanMap map = new OceanMap(6, 6, Set.of(new Position(0, 4)));
        Ship player = new Ship(new Position(1, 1), map);
        PirateShip pirate = new PirateShip(new Position(0, 5), map);
        player.addObserver(pirate);

        player.move(Direction.EAST);

        assertEquals(new Position(1, 5), pirate.getPosition());
    }

    @Test
    void staysPutWhenBothChaseDirectionsAreBlocked() {
        OceanMap map = new OceanMap(5, 5, Set.of(new Position(3, 0), new Position(4, 1)));
        Ship player = new Ship(new Position(0, 1), map);
        PirateShip pirate = new PirateShip(new Position(4, 0), map);
        player.addObserver(pirate);

        player.move(Direction.SOUTH);

        assertEquals(new Position(4, 0), pirate.getPosition());
    }

    @Test
    void doesNotMoveWhenPlayerMoveIsBlocked() {
        OceanMap map = new OceanMap(4, 4, Set.of(new Position(1, 0)));
        Ship player = new Ship(new Position(0, 0), map);
        PirateShip pirate = new PirateShip(new Position(3, 3), map);
        player.addObserver(pirate);

        player.move(Direction.EAST);

        assertEquals(new Position(3, 3), pirate.getPosition());
    }

    @Test
    void staysOnPlayerCellWhenPlayerMovesOntoPirate() {
        OceanMap map = new OceanMap(3, 3, Set.of());
        Ship player = new Ship(new Position(0, 0), map);
        PirateShip pirate = new PirateShip(new Position(1, 0), map);
        player.addObserver(pirate);

        player.move(Direction.EAST);

        assertEquals(new Position(1, 0), pirate.getPosition());
    }

    @ParameterizedTest
    @CsvSource({"-1,0", "3,0", "1,1"})
    void rejectsStartingOutsideWater(int x, int y) {
        OceanMap map = new OceanMap(3, 3, Set.of(new Position(1, 1)));
        assertThrows(IllegalArgumentException.class, () -> new PirateShip(new Position(x, y), map));
    }
}
