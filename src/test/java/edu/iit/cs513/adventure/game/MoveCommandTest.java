package edu.iit.cs513.adventure.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MoveCommandTest {
    @Test
    void commandEncapsulatesItsDirectionAndCanBeExecutedAgain() {
        Game game = new Game(new OceanMap(3, 3, Set.of()), new Position(0, 0), List.of(), new Position(2, 2));
        GameCommand command = new MoveCommand(Direction.SOUTH);

        assertTrue(command.execute(game));
        assertEquals(new Position(0, 1), game.getPlayerPosition());
        assertTrue(command.execute(game));
        assertEquals(new Position(0, 2), game.getPlayerPosition());
        assertEquals(2, game.getTurnCount());
    }

    @Test
    void commandRespectsBlockedMovementAndTerminalGameStates() {
        Game game = new Game(new OceanMap(2, 1, Set.of()), new Position(0, 0), List.of(), new Position(1, 0));

        assertFalse(new MoveCommand(Direction.WEST).execute(game));
        assertEquals(0, game.getTurnCount());
        assertTrue(new MoveCommand(Direction.EAST).execute(game));
        assertFalse(new MoveCommand(Direction.WEST).execute(game));
        assertEquals(GameState.WON, game.getState());
        assertEquals(1, game.getTurnCount());
    }

    @Test
    void rejectsMissingDirection() {
        assertThrows(NullPointerException.class, () -> new MoveCommand(null));
    }
}
