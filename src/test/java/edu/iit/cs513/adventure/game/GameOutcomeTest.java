package edu.iit.cs513.adventure.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GameOutcomeTest {
    @Test
    void ordinaryMoveKeepsGamePlayingAndAdvancesOneTurn() {
        Game game = new Game(new OceanMap(4, 4, Set.of()), new Position(0, 0),
                List.of(new Position(3, 3)), new Position(0, 3));
        assertEquals(GameState.PLAYING, game.getState());
        assertEquals(0, game.getTurnCount());

        assertTrue(game.move(Direction.EAST));

        assertEquals(GameState.PLAYING, game.getState());
        assertEquals(1, game.getTurnCount());
        assertEquals(new Position(3, 2), game.getPiratePositions().getFirst());
    }

    @Test
    void reachingTreasureSafelyWinsAndFreezesSubsequentTurns() {
        Game game = new Game(new OceanMap(5, 5, Set.of()), new Position(0, 0),
                List.of(new Position(4, 4)), new Position(1, 0));

        assertTrue(game.move(Direction.EAST));
        assertEquals(GameState.WON, game.getState());
        assertEquals(new Position(1, 0), game.getPlayerPosition());
        assertEquals(1, game.getTurnCount());
        List<Position> pirates = game.getPiratePositions();

        for (Direction direction : Direction.values()) {
            assertFalse(game.move(direction));
            assertEquals(GameState.WON, game.getState());
            assertEquals(new Position(1, 0), game.getPlayerPosition());
            assertEquals(pirates, game.getPiratePositions());
            assertEquals(1, game.getTurnCount());
        }
    }

    @Test
    void pirateCaptureLosesAndFreezesSubsequentTurns() {
        Game game = new Game(new OceanMap(5, 5, Set.of()), new Position(0, 0),
                List.of(new Position(4, 0)), new Position(4, 4));
        assertTrue(game.move(Direction.EAST));
        assertEquals(GameState.PLAYING, game.getState());

        assertTrue(game.move(Direction.EAST));
        assertEquals(GameState.LOST, game.getState());
        assertEquals(List.of(new Position(2, 0)), game.getPiratePositions());
        assertEquals(2, game.getTurnCount());

        for (Direction direction : Direction.values()) {
            assertFalse(game.move(direction));
            assertEquals(GameState.LOST, game.getState());
            assertEquals(new Position(2, 0), game.getPlayerPosition());
            assertEquals(List.of(new Position(2, 0)), game.getPiratePositions());
            assertEquals(2, game.getTurnCount());
        }
    }

    @Test
    void sailingDirectlyOntoPirateLoses() {
        Game game = new Game(new OceanMap(3, 3, Set.of()), new Position(0, 0),
                List.of(new Position(1, 0)), new Position(2, 2));

        assertTrue(game.move(Direction.EAST));

        assertEquals(GameState.LOST, game.getState());
        assertEquals(game.getPlayerPosition(), game.getPiratePositions().getFirst());
    }

    @Test
    void captureTakesPriorityOverTreasureOnTheSameTurn() {
        Game game = new Game(new OceanMap(3, 3, Set.of()), new Position(0, 0),
                List.of(new Position(2, 0)), new Position(1, 0));

        assertTrue(game.move(Direction.EAST));

        assertEquals(game.getTreasurePosition(), game.getPlayerPosition());
        assertEquals(GameState.LOST, game.getState());
    }

    @Test
    void blockedMovesDoNotAdvanceTurnsOrChangeGameState() {
        Game game = new Game(new OceanMap(4, 4, Set.of(new Position(1, 0))),
                new Position(0, 0), List.of(new Position(3, 3)), new Position(0, 3));

        assertFalse(game.move(Direction.EAST));
        assertFalse(game.move(Direction.NORTH));

        assertEquals(GameState.PLAYING, game.getState());
        assertEquals(0, game.getTurnCount());
        assertEquals(new Position(0, 0), game.getPlayerPosition());
        assertEquals(List.of(new Position(3, 3)), game.getPiratePositions());
    }

    @ParameterizedTest
    @CsvSource({"-1,0", "3,0", "0,-1", "0,3", "1,1", "0,0", "2,2"})
    void rejectsTreasureOutsideWaterOrOverlappingShips(int x, int y) {
        OceanMap map = new OceanMap(3, 3, Set.of(new Position(1, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> new Game(map, new Position(0, 0), List.of(new Position(2, 2)), new Position(x, y)));
    }

    @Test
    void rejectsMissingTreasure() {
        assertThrows(IllegalArgumentException.class,
                () -> new Game(new OceanMap(3, 3, Set.of()), new Position(0, 0), List.of(), null));
    }

    @Test
    void rejectsTreasureCutOffByAnIslandBarrier() {
        OceanMap map = new OceanMap(3, 3,
                Set.of(new Position(1, 0), new Position(1, 1), new Position(1, 2)));

        assertThrows(IllegalArgumentException.class,
                () -> new Game(map, new Position(0, 0), List.of(), new Position(2, 0)));
    }

    @Test
    void treasureCanBeReachedByADetourAroundIslands() {
        OceanMap map = new OceanMap(3, 3, Set.of(new Position(1, 0), new Position(1, 1)));
        Game game = new Game(map, new Position(0, 0), List.of(), new Position(2, 0));

        for (Direction direction : List.of(Direction.SOUTH, Direction.SOUTH, Direction.EAST,
                Direction.EAST, Direction.NORTH, Direction.NORTH)) {
            assertTrue(game.move(direction));
        }

        assertEquals(GameState.WON, game.getState());
        assertEquals(new Position(2, 0), game.getPlayerPosition());
        assertEquals(6, game.getTurnCount());
    }

    @Test
    void supportsTreasureInSmallCustomMaps() {
        Game game = new Game(new OceanMap(2, 1, Set.of()), new Position(0, 0),
                List.of(), new Position(1, 0));

        assertTrue(game.move(Direction.EAST));

        assertEquals(GameState.WON, game.getState());
    }
}
