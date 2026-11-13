package edu.iit.cs513.adventure.game;

/** Assignment 2's larger-axis chase, with a fallback when the preferred step is blocked. */
public final class GreedyChaseStrategy implements PirateMovementStrategy {
    @Override
    public Position nextPosition(Position current, Position target, Position previousTarget, OceanMap map) {
        int dx = target.x() - current.x();
        int dy = target.y() - current.y();
        Position horizontal = new Position(current.x() + Integer.signum(dx), current.y());
        Position vertical = new Position(current.x(), current.y() + Integer.signum(dy));
        Position first = Math.abs(dx) >= Math.abs(dy) ? horizontal : vertical;
        Position second = Math.abs(dx) >= Math.abs(dy) ? vertical : horizontal;
        if (!first.equals(current) && map.isNavigable(first)) {
            return first;
        }
        return map.isNavigable(second) ? second : current;
    }
}
