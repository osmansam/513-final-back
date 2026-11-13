package edu.iit.cs513.adventure.game;

import java.util.List;
import java.util.Objects;

/** Immutable game view suitable for a separate frontend; no live entities are exposed. */
public record GameSnapshot(int width, int height, List<Position> islands, Position player, Position treasure,
                           List<PirateSnapshot> pirates, List<MonsterSnapshot> monsters,
                           GameState state, int turnCount) {
    public GameSnapshot {
        islands = List.copyOf(islands);
        pirates = List.copyOf(pirates);
        monsters = List.copyOf(monsters);
        Objects.requireNonNull(player);
        Objects.requireNonNull(treasure);
        Objects.requireNonNull(state);
    }
}
