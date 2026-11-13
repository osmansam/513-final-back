package edu.iit.cs513.adventure.game;

/** Finds a shortest water route, including detours that temporarily increase distance to the player. */
public final class ShortestPathChaseStrategy implements PirateMovementStrategy {
    @Override
    public Position nextPosition(Position current, Position target, Position previousTarget, OceanMap map) {
        return map.stepTowards(current, target);
    }
}
