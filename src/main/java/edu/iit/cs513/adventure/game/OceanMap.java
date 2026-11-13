package edu.iit.cs513.adventure.game;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Immutable ocean terrain shared by the player and enemy ships. */
public final class OceanMap {
    private final int width;
    private final int height;
    private final Set<Position> islands;

    public OceanMap(int width, int height, Set<Position> islands) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Map dimensions must be positive");
        }
        this.width = width;
        this.height = height;
        this.islands = Set.copyOf(islands);
        if (this.islands.stream().anyMatch(position -> !contains(position))) {
            throw new IllegalArgumentException("Islands must be inside the map");
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Set<Position> getIslands() {
        return islands;
    }

    public boolean contains(Position position) {
        return position != null && position.x() >= 0 && position.x() < width
                && position.y() >= 0 && position.y() < height;
    }

    public boolean isIsland(Position position) {
        return position != null && islands.contains(position);
    }

    public boolean isNavigable(Position position) {
        return contains(position) && !isIsland(position);
    }

    /** Finds the water region accessible using the same four directions as the ships. */
    Set<Position> reachableFrom(Position start) {
        if (!isNavigable(start)) {
            return Set.of();
        }
        Set<Position> visited = new HashSet<>();
        ArrayDeque<Position> pending = new ArrayDeque<>();
        visited.add(start);
        pending.add(start);
        while (!pending.isEmpty()) {
            Position current = pending.remove();
            for (Direction direction : Direction.values()) {
                Position next = direction.moveFrom(current);
                if (isNavigable(next) && visited.add(next)) {
                    pending.add(next);
                }
            }
        }
        return Set.copyOf(visited);
    }

    /** Breadth-first search returns a single shortest-route step, or the start if no route exists. */
    Position stepTowards(Position start, Position target) {
        if (!isNavigable(start) || !isNavigable(target) || start.equals(target)) {
            return start;
        }
        Map<Position, Position> firstSteps = new HashMap<>();
        ArrayDeque<Position> pending = new ArrayDeque<>();
        firstSteps.put(start, start);
        pending.add(start);
        while (!pending.isEmpty()) {
            Position current = pending.remove();
            for (Direction direction : Direction.values()) {
                Position next = direction.moveFrom(current);
                if (isNavigable(next) && !firstSteps.containsKey(next)) {
                    Position firstStep = current.equals(start) ? next : firstSteps.get(current);
                    if (next.equals(target)) {
                        return firstStep;
                    }
                    firstSteps.put(next, firstStep);
                    pending.add(next);
                }
            }
        }
        return start;
    }
}
