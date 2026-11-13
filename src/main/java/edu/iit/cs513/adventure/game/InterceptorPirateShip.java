package edu.iit.cs513.adventure.game;

public final class InterceptorPirateShip extends PirateShip {
    public InterceptorPirateShip(Position start, OceanMap map) {
        super(start, map, new InterceptStrategy());
    }

    @Override
    public PirateType getType() {
        return PirateType.INTERCEPTOR;
    }
}
