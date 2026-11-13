package edu.iit.cs513.adventure.game;

/** Inclusive bounds for a monster's patrol area. */
public record RoamingArea(int minX, int minY, int maxX, int maxY) {
    public RoamingArea {
        if (minX < 0 || minY < 0 || maxX < minX || maxY < minY) {
            throw new IllegalArgumentException("Roaming bounds must be nonnegative and ordered");
        }
    }

    public boolean contains(Position position) {
        return position != null && position.x() >= minX && position.x() <= maxX
                && position.y() >= minY && position.y() <= maxY;
    }
}
