package edu.iit.cs513.adventure.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GameMonsterTest {
    @Test
    void monstersAdvanceOnSuccessfulMovesButNotBlockedMoves() {
        Game game = new Game(new OceanMap(6, 6, Set.of()), new Position(0, 0), List.of(),
                new Position(5, 5), List.of(new MonsterSpawn(new Position(3, 3), new RoamingArea(2, 2, 4, 4))));

        assertTrue(game.move(Direction.EAST));
        assertEquals(List.of(new Position(4, 3)), game.getMonsterPositions());
        assertFalse(game.move(Direction.NORTH));
        assertEquals(List.of(new Position(4, 3)), game.getMonsterPositions());
        assertEquals(1, game.getTurnCount());
    }

    @Test
    void monsterSwimmingOntoPlayerLosesAndFreezesTheGame() {
        Game game = new Game(new OceanMap(4, 4, Set.of()), new Position(1, 0), List.of(),
                new Position(3, 3), List.of(new MonsterSpawn(new Position(0, 1), new RoamingArea(0, 1, 2, 2))));

        assertTrue(game.move(Direction.SOUTH));
        assertEquals(GameState.LOST, game.getState());
        assertEquals(List.of(new Position(1, 1)), game.getMonsterPositions());
        assertFalse(game.move(Direction.EAST));
        assertEquals(new Position(1, 1), game.getPlayerPosition());
        assertEquals(List.of(new Position(1, 1)), game.getMonsterPositions());
        assertEquals(1, game.getTurnCount());
    }

    @Test
    void enteringAMonsterCellLosesBeforeTheMonsterCanSwimAway() {
        Game game = new Game(new OceanMap(4, 4, Set.of()), new Position(0, 0), List.of(),
                new Position(3, 3), List.of(new MonsterSpawn(new Position(1, 0), new RoamingArea(1, 0, 3, 2))));

        assertTrue(game.move(Direction.EAST));

        assertEquals(GameState.LOST, game.getState());
        assertEquals(List.of(new Position(1, 0)), game.getMonsterPositions());
    }

    @Test
    void monsterCaptureTakesPriorityOverTreasure() {
        Game game = new Game(new OceanMap(4, 4, Set.of()), new Position(1, 0), List.of(),
                new Position(1, 1), List.of(new MonsterSpawn(new Position(0, 1), new RoamingArea(0, 1, 2, 2))));

        game.move(Direction.SOUTH);

        assertEquals(game.getTreasurePosition(), game.getPlayerPosition());
        assertEquals(GameState.LOST, game.getState());
    }

    @Test
    void winningFreezesMonsterPositionsAsWellAsShips() {
        Game game = new Game(new OceanMap(6, 6, Set.of()), new Position(0, 0), List.of(),
                new Position(1, 0), List.of(new MonsterSpawn(new Position(3, 3), new RoamingArea(2, 2, 4, 4))));

        game.move(Direction.EAST);
        List<Position> snapshot = game.getMonsterPositions();
        assertEquals(GameState.WON, game.getState());
        assertFalse(game.move(Direction.SOUTH));
        assertEquals(snapshot, game.getMonsterPositions());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
    }

    @Test
    void rejectsMonsterSpawnsOverlappingAnyEntityOrAnotherMonster() {
        OceanMap map = new OceanMap(5, 5, Set.of());
        RoamingArea area = new RoamingArea(0, 0, 4, 4);
        for (Position occupied : List.of(new Position(0, 0), new Position(2, 2), new Position(4, 4))) {
            assertThrows(IllegalArgumentException.class, () -> new Game(map, new Position(0, 0),
                    List.of(new Position(2, 2)), new Position(4, 4), List.of(new MonsterSpawn(occupied, area))));
        }
        MonsterSpawn duplicate = new MonsterSpawn(new Position(3, 3), area);
        assertThrows(IllegalArgumentException.class, () -> new Game(map, new Position(0, 0),
                List.of(), new Position(4, 4), List.of(duplicate, duplicate)));
    }
}
