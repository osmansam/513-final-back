package edu.iit.cs513.adventure.game;

import java.util.Objects;
import java.util.Observable;
import java.util.Observer;

/** Observes the player's position and delegates each move to an interchangeable strategy. */
@SuppressWarnings("deprecation")
public class PirateShip implements Observer {
    private final OceanMap oceanMap;
    private final PirateMovementStrategy strategy;
    private Position position;
    private Position previousTarget;

    public PirateShip(Position position, OceanMap oceanMap) {
        this(position, oceanMap, new GreedyChaseStrategy());
    }

    public PirateShip(Position position, OceanMap oceanMap, PirateMovementStrategy strategy) {
        this.oceanMap = Objects.requireNonNull(oceanMap);
        this.strategy = Objects.requireNonNull(strategy);
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
            // Capture is immediate when the player sails onto a pirate's existing cell.
            if (!position.equals(target)) {
                Position next = strategy.nextPosition(position, target, previousTarget, oceanMap);
                if (oceanMap.isNavigable(next)
                        && Math.abs(next.x() - position.x()) + Math.abs(next.y() - position.y()) <= 1) {
                    position = next;
                }
            }
            previousTarget = target;
        }
    }
}
