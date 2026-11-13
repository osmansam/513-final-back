package edu.iit.cs513.adventure.game;

public final class HunterPirateFactory extends PirateShipFactory {
    @Override
    public PirateShip create(Position start, OceanMap map) {
        return new HunterPirateShip(start, map);
    }
}
