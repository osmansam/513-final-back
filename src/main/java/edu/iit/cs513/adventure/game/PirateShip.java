package edu.iit.cs513.adventure.game;

import java.util.Objects;
import java.util.Observable;
import java.util.Observer;

/** Observer-based pirate with Assignment 2's greedy chase and alternate-axis fallback. */
@SuppressWarnings("deprecation")
public final class PirateShip implements Observer {
    private final OceanMap oceanMap;
    private Position position;

    public PirateShip(Position position, OceanMap oceanMap) {
        this.oceanMap = Objects.requireNonNull(oceanMap);
        if (!oceanMap.isNavigable(position)) {
            throw new IllegalArgumentException("Pirate must start on a water cell");
        }
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    @Override
    public void update(Observable source, Object location) {
        if (source instanceof Ship && location instanceof Position target) {
            chase(target);
        }
    }

    private void chase(Position target) {
        int dx = target.x() - position.x();
        int dy = target.y() - position.y();
        if (Math.abs(dx) >= Math.abs(dy)) {
            if (!tryMove(Integer.signum(dx), 0)) {
                tryMove(0, Integer.signum(dy));
            }
        } else if (!tryMove(0, Integer.signum(dy))) {
            tryMove(Integer.signum(dx), 0);
        }
    }

    private boolean tryMove(int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return false;
        }
        Position destination = new Position(position.x() + dx, position.y() + dy);
        if (!oceanMap.isNavigable(destination)) {
            return false;
        }
        position = destination;
        return true;
    }
}
