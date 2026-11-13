package edu.iit.cs513.adventure.game;

/** Navigates toward a point two cells ahead of the player's most recent heading. */
public final class InterceptStrategy implements PirateMovementStrategy {
    @Override
    public Position nextPosition(Position current, Position target, Position previousTarget, OceanMap map) {
        int distance = Math.abs(current.x() - target.x()) + Math.abs(current.y() - target.y());
        if (distance <= 1 || previousTarget == null) {
            return map.stepTowards(current, target);
        }
        int dx = target.x() - previousTarget.x();
        int dy = target.y() - previousTarget.y();
        if (Math.abs(dx) + Math.abs(dy) != 1) {
            return map.stepTowards(current, target);
        }
        Position projected = target;
        for (int step = 0; step < 2; step++) {
            Position next = new Position(projected.x() + dx, projected.y() + dy);
            if (!map.isNavigable(next)) {
                break;
            }
            projected = next;
        }
        return map.stepTowards(current, projected);
    }
}
