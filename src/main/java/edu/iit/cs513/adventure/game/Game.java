package edu.iit.cs513.adventure.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/** Owns one game's terrain and ships, independently of the HTTP server. */
@SuppressWarnings("deprecation")
public final class Game {
    private static final int DEFAULT_WIDTH = 20;
    private static final int DEFAULT_HEIGHT = 20;
    private static final int ISLAND_COUNT = 40;
    private static final int PIRATE_COUNT = 2;

    private final OceanMap oceanMap;
    private final Ship player;
    private final List<PirateShip> pirates;

    /** Builds a specified layout, also usable by a future game design interface. */
    public Game(OceanMap oceanMap, Position playerStart, List<Position> pirateStarts) {
        this.oceanMap = Objects.requireNonNull(oceanMap);
        player = new Ship(playerStart, oceanMap);
        Set<Position> occupied = new HashSet<>();
        occupied.add(playerStart);
        List<PirateShip> ships = new ArrayList<>();
        for (Position start : pirateStarts) {
            if (!occupied.add(start)) {
                throw new IllegalArgumentException("Ships must start on distinct cells");
            }
            ships.add(new PirateShip(start, oceanMap));
        }
        pirates = List.copyOf(ships);
        pirates.forEach(player::addObserver);
    }

    public static Game newGame() {
        return generate(new Random());
    }

    /** Reproducible layouts make gameplay bugs and tests easier to reproduce. */
    public static Game newGame(long seed) {
        return generate(new Random(seed));
    }

    private static Game generate(Random random) {
        List<Position> cells = new ArrayList<>();
        for (int y = 0; y < DEFAULT_HEIGHT; y++) {
            for (int x = 0; x < DEFAULT_WIDTH; x++) {
                cells.add(new Position(x, y));
            }
        }
        // Sampling without replacement guarantees distinct spawns and bounded generation time.
        Collections.shuffle(cells, random);
        Position playerStart = cells.getFirst();
        List<Position> pirateStarts = cells.subList(1, 1 + PIRATE_COUNT);
        Set<Position> islands = new HashSet<>(cells.subList(1 + PIRATE_COUNT,
                1 + PIRATE_COUNT + ISLAND_COUNT));
        return new Game(new OceanMap(DEFAULT_WIDTH, DEFAULT_HEIGHT, islands), playerStart, pirateStarts);
    }

    public OceanMap getMap() {
        return oceanMap;
    }

    public Position getPlayerPosition() {
        return player.getPosition();
    }

    public List<Position> getPiratePositions() {
        return pirates.stream().map(PirateShip::getPosition).toList();
    }

    public boolean move(Direction direction) {
        return player.move(direction);
    }
}
