package edu.iit.cs513.adventure.game;

import java.util.Objects;

/** Patrols forward until blocked, then tries clockwise turns within its area. */
public final class SeaMonster {
    private static final Direction[] HEADINGS = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
    private final OceanMap map;
    private final RoamingArea area;
    private Position position;
    private int heading;

    public SeaMonster(Position start, OceanMap map, RoamingArea area) {
        this.map = Objects.requireNonNull(map);
        this.area = Objects.requireNonNull(area);
        if (area.maxX() >= map.getWidth() || area.maxY() >= map.getHeight()
                || !area.contains(start) || !map.isNavigable(start)) {
            throw new IllegalArgumentException("Monster must start on water inside a roaming area within the map");
        }
        position = start;
    }

    public Position getPosition() {
        return position;
    }

    public RoamingArea getArea() {
        return area;
    }

    public void move() {
        for (int offset = 0; offset < HEADINGS.length; offset++) {
            int nextHeading = (heading + offset) % HEADINGS.length;
            Position next = HEADINGS[nextHeading].moveFrom(position);
            if (area.contains(next) && map.isNavigable(next)) {
                position = next;
                heading = nextHeading;
                return;
            }
        }
    }
}
