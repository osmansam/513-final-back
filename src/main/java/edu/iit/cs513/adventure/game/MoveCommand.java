package edu.iit.cs513.adventure.game;

import java.util.Objects;

public record MoveCommand(Direction direction) implements GameCommand {
    public MoveCommand {
        Objects.requireNonNull(direction, "Direction is required");
    }

    @Override
    public boolean execute(Game game) {
        return game.move(direction);
    }
}
