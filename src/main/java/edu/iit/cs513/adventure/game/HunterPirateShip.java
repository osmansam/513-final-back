package edu.iit.cs513.adventure.game;

public final class HunterPirateShip extends PirateShip {
    public HunterPirateShip(Position start, OceanMap map) {
        super(start, map, new ShortestPathChaseStrategy());
    }
}
