package edu.iit.cs513.adventure.game;

/** Strategy for choosing a pirate's next cell; previousTarget is null on its first notification. */
@FunctionalInterface
public interface PirateMovementStrategy {
    Position nextPosition(Position current, Position target, Position previousTarget, OceanMap map);
}
