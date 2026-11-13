package edu.iit.cs513.adventure.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GameSessionTest {
    @Test
    void appliedMoveReturnsTheResultingSnapshot() {
        GameSession session = new GameSession(GameSessionTest::openGame);

        ActionResult result = session.execute(new MoveCommand(Direction.EAST));

        assertEquals(ActionStatus.APPLIED, result.status());
        assertEquals(new Position(1, 0), result.snapshot().player());
        assertEquals(1, result.snapshot().turnCount());
        assertEquals(session.snapshot(), result.snapshot());
    }

    @Test
    void blockedMoveHasADistinctStatusAndLeavesTheSnapshotUnchanged() {
        GameSession session = new GameSession(GameSessionTest::openGame);
        GameSnapshot before = session.snapshot();

        ActionResult result = session.execute(new MoveCommand(Direction.NORTH));

        assertEquals(ActionStatus.BLOCKED, result.status());
        assertEquals(before, result.snapshot());
    }

    @Test
    void rejectsFurtherCommandsAfterWinning() {
        GameSession session = new GameSession(() -> new Game(new OceanMap(2, 1, Set.of()),
                new Position(0, 0), List.of(), new Position(1, 0)));

        ActionResult win = session.execute(new MoveCommand(Direction.EAST));
        ActionResult rejected = session.execute(new MoveCommand(Direction.WEST));

        assertEquals(ActionStatus.APPLIED, win.status());
        assertEquals(GameState.WON, win.snapshot().state());
        assertEquals(ActionStatus.GAME_OVER, rejected.status());
        assertEquals(win.snapshot(), rejected.snapshot());
        assertEquals(new Position(0, 0), session.reset().player());
        assertEquals(GameState.WON, session.execute(new MoveCommand(Direction.EAST)).snapshot().state());
    }

    @Test
    void resettingALostGameRestoresEnemiesAndClearsTheTurnCount() {
        GameSession session = new GameSession(() -> new Game(new OceanMap(5, 3, Set.of()),
                new Position(0, 0), List.of(new Position(2, 0)), new Position(4, 2),
                List.of(new MonsterSpawn(new Position(2, 2), new RoamingArea(1, 1, 3, 2)))));
        GameSnapshot initial = session.snapshot();
        ActionResult loss = session.execute(new MoveCommand(Direction.EAST));
        assertEquals(GameState.LOST, loss.snapshot().state());
        assertEquals(ActionStatus.GAME_OVER, session.execute(new MoveCommand(Direction.SOUTH)).status());

        assertEquals(initial, session.reset());
        ActionResult nextLoss = session.execute(new MoveCommand(Direction.EAST));
        assertEquals(loss, nextLoss);
    }

    @Test
    void resettingAnActiveGameRestoresItsStartingState() {
        GameSession session = new GameSession(GameSessionTest::openGame);
        GameSnapshot initial = session.snapshot();
        session.execute(new MoveCommand(Direction.EAST));

        assertEquals(initial, session.reset());
        assertEquals(0, session.snapshot().turnCount());
        assertEquals(GameState.PLAYING, session.snapshot().state());
    }

    @Test
    void seededSessionsReplayTheSameInitialLayoutOnReset() {
        GameSession session = new GameSession(513);
        GameSnapshot initial = session.snapshot();
        boolean moved = false;
        for (Direction direction : Direction.values()) {
            if (session.execute(new MoveCommand(direction)).status() == ActionStatus.APPLIED) {
                moved = true;
                break;
            }
        }

        assertTrue(moved);
        assertEquals(initial, session.reset());
        assertEquals(initial, new GameSession(513).snapshot());
    }

    @Test
    void eachResetCreatesTheNextGameFromTheConfiguredFactory() {
        AtomicInteger starts = new AtomicInteger();
        GameSession session = new GameSession(() -> new Game(new OceanMap(4, 2, Set.of()),
                new Position(starts.getAndIncrement(), 0), List.of(), new Position(3, 1)));

        assertEquals(new Position(0, 0), session.snapshot().player());
        assertEquals(new Position(1, 0), session.reset().player());
        assertEquals(new Position(2, 0), session.reset().player());
    }

    @Test
    void failedResetPreservesTheCurrentGame() {
        AtomicInteger creations = new AtomicInteger();
        GameSession session = new GameSession(() -> {
            if (creations.getAndIncrement() > 0) {
                throw new IllegalStateException("Cannot create a new game");
            }
            return openGame();
        });
        session.execute(new MoveCommand(Direction.EAST));
        GameSnapshot before = session.snapshot();

        assertThrows(IllegalStateException.class, session::reset);
        assertEquals(before, session.snapshot());
    }

    @Test
    void differentSessionsOwnIndependentGames() {
        GameSession first = new GameSession(GameSessionTest::openGame);
        GameSession second = new GameSession(GameSessionTest::openGame);

        first.execute(new MoveCommand(Direction.EAST));

        assertEquals(new Position(0, 0), second.snapshot().player());
        assertEquals(0, second.snapshot().turnCount());
        first.reset();
        assertEquals(second.snapshot(), first.snapshot());
    }

    @Test
    void concurrentCommandsReturnSeparateCompleteTurnsWithoutLostUpdates() throws Exception {
        GameSession session = new GameSession(() -> new Game(new OceanMap(100, 1, Set.of()),
                new Position(0, 0), List.of(), new Position(99, 0)));
        List<Callable<ActionResult>> commands = new ArrayList<>();
        for (int index = 0; index < 80; index++) {
            commands.add(() -> session.execute(new MoveCommand(Direction.EAST)));
        }
        Set<Integer> turns = new HashSet<>();
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (var future : executor.invokeAll(commands, 5, TimeUnit.SECONDS)) {
                ActionResult result = future.get();
                assertEquals(ActionStatus.APPLIED, result.status());
                assertEquals(result.snapshot().turnCount(), result.snapshot().player().x());
                turns.add(result.snapshot().turnCount());
            }
        }

        assertEquals(80, turns.size());
        assertEquals(80, session.snapshot().turnCount());
        assertEquals(new Position(80, 0), session.snapshot().player());
    }

    private static Game openGame() {
        return new Game(new OceanMap(5, 3, Set.of()), new Position(0, 0), List.of(), new Position(4, 2));
    }
}
