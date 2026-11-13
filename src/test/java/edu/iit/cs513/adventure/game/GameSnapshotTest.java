package edu.iit.cs513.adventure.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GameSnapshotTest {
    @Test
    void snapshotContainsTheMapAndTypedEntitiesInStableOrder() {
        Game game = new Game(new OceanMap(4, 3,
                Set.of(new Position(2, 0), new Position(1, 2), new Position(0, 2))),
                new Position(0, 0), List.of(new Position(3, 0)), new Position(3, 2),
                List.of(new MonsterSpawn(new Position(2, 2), new RoamingArea(2, 1, 3, 2))));

        GameSnapshot snapshot = game.snapshot();

        assertEquals(4, snapshot.width());
        assertEquals(3, snapshot.height());
        assertEquals(List.of(new Position(2, 0), new Position(0, 2), new Position(1, 2)), snapshot.islands());
        assertEquals(new Position(0, 0), snapshot.player());
        assertEquals(new Position(3, 2), snapshot.treasure());
        assertEquals(List.of(new PirateSnapshot("pirate-1", PirateType.STANDARD, new Position(3, 0))),
                snapshot.pirates());
        assertEquals(List.of(new MonsterSnapshot("monster-1", new Position(2, 2), new RoamingArea(2, 1, 3, 2))),
                snapshot.monsters());
        assertEquals(GameState.PLAYING, snapshot.state());
        assertEquals(0, snapshot.turnCount());
    }

    @Test
    void oldSnapshotsKeepTheirValuesAndEntityIdsAcrossTurns() {
        Game game = new Game(new OceanMap(6, 6, Set.of()), new Position(0, 0),
                List.of(new Position(5, 0)), new Position(5, 5),
                List.of(new MonsterSpawn(new Position(3, 3), new RoamingArea(2, 2, 4, 4))));
        GameSnapshot before = game.snapshot();

        game.move(Direction.EAST);
        GameSnapshot after = game.snapshot();

        assertEquals(new Position(0, 0), before.player());
        assertEquals(new Position(5, 0), before.pirates().getFirst().position());
        assertEquals(new Position(3, 3), before.monsters().getFirst().position());
        assertEquals(0, before.turnCount());
        assertEquals(new Position(1, 0), after.player());
        assertEquals(new Position(4, 0), after.pirates().getFirst().position());
        assertEquals(new Position(4, 3), after.monsters().getFirst().position());
        assertEquals(before.pirates().getFirst().id(), after.pirates().getFirst().id());
        assertEquals(before.monsters().getFirst().id(), after.monsters().getFirst().id());
    }

    @Test
    void snapshotDefensivelyCopiesEveryCollection() {
        List<Position> islands = new ArrayList<>(List.of(new Position(1, 1)));
        List<PirateSnapshot> pirates = new ArrayList<>(List.of(
                new PirateSnapshot("pirate-1", PirateType.STANDARD, new Position(2, 0))));
        List<MonsterSnapshot> monsters = new ArrayList<>(List.of(
                new MonsterSnapshot("monster-1", new Position(2, 2), new RoamingArea(1, 1, 2, 2))));
        GameSnapshot snapshot = new GameSnapshot(3, 3, islands, new Position(0, 0), new Position(0, 2),
                pirates, monsters, GameState.PLAYING, 0);
        islands.clear();
        pirates.clear();
        monsters.clear();

        assertEquals(1, snapshot.islands().size());
        assertEquals(1, snapshot.pirates().size());
        assertEquals(1, snapshot.monsters().size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.islands().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.pirates().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.monsters().clear());
    }

    @Test
    void generatedSnapshotIdentifiesTheTwoDifferentPirateProducts() {
        GameSnapshot snapshot = Game.newGame(513).snapshot();

        assertEquals(List.of(PirateType.HUNTER, PirateType.INTERCEPTOR),
                snapshot.pirates().stream().map(PirateSnapshot::type).toList());
        Set<String> ids = new HashSet<>();
        snapshot.pirates().forEach(pirate -> ids.add(pirate.id()));
        snapshot.monsters().forEach(monster -> ids.add(monster.id()));
        assertEquals(4, ids.size());
    }
}
