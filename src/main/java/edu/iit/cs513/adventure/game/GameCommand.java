package edu.iit.cs513.adventure.game;

/** Command pattern: a player action is a value executed against the game receiver. */
public sealed interface GameCommand permits MoveCommand {
    boolean execute(Game game);
}
