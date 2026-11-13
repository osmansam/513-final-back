package edu.iit.cs513.adventure.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GameTest {
    @Test
    void gameAutomaticallyRegistersAllPiratesAsObservers() {
        Game game = new Game(new OceanMap(6, 6, Set.of()), new Position(0, 0),
                List.of(new Position(5, 0), new Position(0, 5)));

        assertTrue(game.move(Direction.EAST));
        assertEquals(new Position(1, 0), game.getPlayerPosition());
        assertEquals(List.of(new Position(4, 0), new Position(0, 4)), game.getPiratePositions());
        assertFalse(game.move(Direction.NORTH));
        assertEquals(List.of(new Position(4, 0), new Position(0, 4)), game.getPiratePositions());
    }

    @Test
    void generatedGamesHaveDistinctWaterSpawnsAndTheRequiredEntities() {
        for (long seed = 0; seed < 50; seed++) {
            Game game = Game.newGame(seed);
            OceanMap map = game.getMap();
            assertEquals(20, map.getWidth());
            assertEquals(20, map.getHeight());
            assertEquals(40, map.getIslands().size());
            assertEquals(2, game.getPiratePositions().size());
            Set<Position> spawns = new HashSet<>(game.getPiratePositions());
            spawns.add(game.getPlayerPosition());
            assertEquals(3, spawns.size());
            assertTrue(spawns.stream().allMatch(map::isNavigable));
        }
    }

    @Test
    void sameSeedRecreatesLayoutForReproducibleGames() {
        Game first = Game.newGame(513);
        Game second = Game.newGame(513);

        assertEquals(first.getMap().getIslands(), second.getMap().getIslands());
        assertEquals(first.getPlayerPosition(), second.getPlayerPosition());
        assertEquals(first.getPiratePositions(), second.getPiratePositions());
        assertEquals(2, Game.newGame().getPiratePositions().size());
    }

    @Test
    void rejectsOverlappingStartingPositions() {
        OceanMap map = new OceanMap(3, 3, Set.of());
        Position player = new Position(0, 0);
        Position pirate = new Position(2, 2);

        assertThrows(IllegalArgumentException.class, () -> new Game(map, player, List.of(player)));
        assertThrows(IllegalArgumentException.class, () -> new Game(map, player, List.of(pirate, pirate)));
    }

    @Test
    void rejectsSpawnsOutsideWater() {
        OceanMap map = new OceanMap(3, 3, Set.of(new Position(1, 1)));

        assertThrows(IllegalArgumentException.class,
                () -> new Game(map, new Position(1, 1), List.of(new Position(2, 2))));
        assertThrows(IllegalArgumentException.class,
                () -> new Game(map, new Position(0, 0), List.of(new Position(3, 2))));
    }

    @Test
    void returnedPiratePositionsAreImmutableSnapshots() {
        List<Position> starts = new ArrayList<>(List.of(new Position(4, 0)));
        Game game = new Game(new OceanMap(5, 5, Set.of()), new Position(0, 0), starts);
        starts.clear();
        List<Position> snapshot = game.getPiratePositions();

        game.move(Direction.EAST);

        assertEquals(List.of(new Position(4, 0)), snapshot);
        assertEquals(List.of(new Position(3, 0)), game.getPiratePositions());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
    }
}
