package edu.iit.cs513.adventure.game;

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
}
