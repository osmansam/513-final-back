package edu.iit.cs513.adventure.game;

/** Factory Method: concrete creators choose the pirate product and its movement policy. */
public abstract class PirateShipFactory {
    public abstract PirateShip create(Position start, OceanMap map);
}
