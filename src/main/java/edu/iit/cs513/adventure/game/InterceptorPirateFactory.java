package edu.iit.cs513.adventure.game;

public final class InterceptorPirateFactory extends PirateShipFactory {
    @Override
    public PirateShip create(Position start, OceanMap map) {
        return new InterceptorPirateShip(start, map);
    }
}
