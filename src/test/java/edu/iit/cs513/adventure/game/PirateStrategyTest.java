package edu.iit.cs513.adventure.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class PirateStrategyTest {
    @Test
    void shortestPathChaseEscapesAnObstacleThatStopsGreedyChase() {
        OceanMap map = new OceanMap(5, 5,
                Set.of(new Position(1, 0), new Position(1, 1), new Position(1, 2)));
        Position start = new Position(2, 1);
        Position target = new Position(0, 1);

        assertEquals(start, new GreedyChaseStrategy().nextPosition(start, target, null, map));
        assertEquals(new Position(2, 2),
                new ShortestPathChaseStrategy().nextPosition(start, target, null, map));
    }

    @Test
    void shortestPathChaseStaysPutWhenTargetIsUnreachable() {
        OceanMap map = new OceanMap(3, 3,
                Set.of(new Position(1, 0), new Position(1, 1), new Position(1, 2)));
        assertEquals(new Position(0, 0), new ShortestPathChaseStrategy()
                .nextPosition(new Position(0, 0), new Position(2, 0), null, map));
    }

    @Test
    void interceptorHeadsAheadOfThePlayersLastMovement() {
        OceanMap map = new OceanMap(5, 5, Set.of());
        Position start = new Position(3, 1);
        Position target = new Position(2, 2);

        assertEquals(new Position(3, 2),
                new ShortestPathChaseStrategy().nextPosition(start, target, new Position(1, 2), map));
        assertEquals(new Position(4, 1),
                new InterceptStrategy().nextPosition(start, target, new Position(1, 2), map));
    }

    @Test
    void interceptorChasesDirectlyUntilItKnowsThePreviousPlayerPosition() {
        assertEquals(new Position(3, 2), new InterceptStrategy().nextPosition(
                new Position(3, 1), new Position(2, 2), null, new OceanMap(5, 5, Set.of())));
    }

    @Test
    void interceptorCapturesAnAdjacentPlayerInsteadOfOvershooting() {
        assertEquals(new Position(2, 2), new InterceptStrategy().nextPosition(
                new Position(2, 1), new Position(2, 2), new Position(1, 2),
                new OceanMap(5, 5, Set.of())));
    }

    @Test
    void interceptorDoesNotProjectThroughAnIslandOrOffTheMap() {
        assertEquals(new Position(2, 1), new InterceptStrategy().nextPosition(
                new Position(3, 1), new Position(2, 2), new Position(1, 2),
                new OceanMap(5, 5, Set.of(new Position(3, 2)))));
        assertEquals(new Position(1, 0), new InterceptStrategy().nextPosition(
                new Position(0, 0), new Position(4, 1), new Position(3, 1),
                new OceanMap(5, 5, Set.of())));
    }

    @Test
    void interceptorStaysPutWhenPlayerAndPredictionAreUnreachable() {
        OceanMap map = new OceanMap(5, 5, Set.of(new Position(1, 0), new Position(1, 1),
                new Position(1, 2), new Position(1, 3), new Position(1, 4)));
        assertEquals(new Position(0, 0), new InterceptStrategy().nextPosition(
                new Position(0, 0), new Position(2, 2), new Position(2, 1), map));
    }
}
