package edu.iit.cs513.adventure.game;

public enum Direction {
    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    Position moveFrom(Position position) {
        return new Position(position.x() + dx, position.y() + dy);
    }
}
