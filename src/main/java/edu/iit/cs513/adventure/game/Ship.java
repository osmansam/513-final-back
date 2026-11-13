package edu.iit.cs513.adventure.game;

import java.util.Objects;
import java.util.Observable;

/** Columbus's ship publishes its location after each successful move, as in Assignment 2. */
@SuppressWarnings("deprecation")
public final class Ship extends Observable {
    private final OceanMap oceanMap;
    private Position position;

    public Ship(Position position, OceanMap oceanMap) {
        this.oceanMap = Objects.requireNonNull(oceanMap);
        if (!oceanMap.isNavigable(position)) {
            throw new IllegalArgumentException("Ship must start on a water cell");
        }
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    /** Returns false for a blocked move; blocked moves do not advance the enemies. */
    public boolean move(Direction direction) {
        Position destination = Objects.requireNonNull(direction).moveFrom(position);
        if (!oceanMap.isNavigable(destination)) {
            return false;
        }
        position = destination;
        setChanged();
        notifyObservers(position);
        return true;
    }
}
