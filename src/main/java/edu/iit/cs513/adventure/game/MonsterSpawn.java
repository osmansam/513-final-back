package edu.iit.cs513.adventure.game;

/** Immutable monster layout data; the game creates and owns the mutable monster. */
public record MonsterSpawn(Position position, RoamingArea area) {
}
