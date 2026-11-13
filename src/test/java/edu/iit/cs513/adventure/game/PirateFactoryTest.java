package edu.iit.cs513.adventure.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
class PirateFactoryTest {
    @Test
    void hunterFactoryCreatesAPirateThatNavigatesAroundIslands() {
        OceanMap map = new OceanMap(5, 5,
                Set.of(new Position(1, 0), new Position(1, 1), new Position(1, 2)));
        PirateShipFactory factory = new HunterPirateFactory();
        PirateShip pirate = factory.create(new Position(2, 1), map);
        Ship player = new Ship(new Position(0, 0), map);
        player.addObserver(pirate);

        player.move(Direction.SOUTH);

        assertInstanceOf(HunterPirateShip.class, pirate);
        assertEquals(new Position(2, 2), pirate.getPosition());
    }

    @Test
    void interceptorFactoryCreatesAPirateThatPredictsTheNextPlayerDirection() {
        OceanMap map = new OceanMap(5, 5, Set.of());
        PirateShipFactory factory = new InterceptorPirateFactory();
        PirateShip pirate = factory.create(new Position(3, 0), map);
        Ship player = new Ship(new Position(0, 2), map);
        player.addObserver(pirate);

        player.move(Direction.EAST);
        assertEquals(new Position(3, 1), pirate.getPosition());
        player.move(Direction.EAST);

        assertInstanceOf(InterceptorPirateShip.class, pirate);
        assertEquals(new Position(4, 1), pirate.getPosition());
    }

    @Test
    void pirateIgnoresStrategyResultsThatTeleportOrCrossTerrain() {
        OceanMap map = new OceanMap(4, 4, Set.of(new Position(2, 3)));
        Ship player = new Ship(new Position(0, 0), map);
        PirateShip teleporting = new PirateShip(new Position(3, 3), map,
                (current, target, previous, ocean) -> target);
        PirateShip crossingIsland = new PirateShip(new Position(3, 3), map,
                (current, target, previous, ocean) -> new Position(2, 3));
        player.addObserver(teleporting);
        player.addObserver(crossingIsland);

        player.move(Direction.EAST);

        assertEquals(new Position(3, 3), teleporting.getPosition());
        assertEquals(new Position(3, 3), crossingIsland.getPosition());
    }

    @Test
    void pirateAlreadyOnThePlayerCellStaysToCaptureEvenIfItsStrategyWouldLeave() {
        OceanMap map = new OceanMap(4, 4, Set.of());
        Ship player = new Ship(new Position(0, 0), map);
        PirateShip pirate = new PirateShip(new Position(1, 0), map,
                (current, target, previous, ocean) -> new Position(2, 0));
        player.addObserver(pirate);

        player.move(Direction.EAST);

        assertEquals(new Position(1, 0), pirate.getPosition());
    }
}
