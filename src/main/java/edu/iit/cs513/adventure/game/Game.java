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
    private final Position treasurePosition;
    private GameState state = GameState.PLAYING;
    private int turnCount;

    /** Builds a layout with distinct spawns and treasure reachable from the player. */
    public Game(OceanMap oceanMap, Position playerStart, List<Position> pirateStarts, Position treasurePosition) {
        this.oceanMap = Objects.requireNonNull(oceanMap);
        this.treasurePosition = treasurePosition;
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
        if (!oceanMap.isNavigable(treasurePosition) || occupied.contains(treasurePosition)) {
            throw new IllegalArgumentException("Treasure must start on a water cell separate from all ships");
        }
        if (!oceanMap.reachableFrom(playerStart).contains(treasurePosition)) {
            throw new IllegalArgumentException("Treasure must be reachable from the player's starting cell");
        }
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
        // Sampling without replacement guarantees distinct islands and bounded generation time.
        Collections.shuffle(cells, random);
        Set<Position> islands = new HashSet<>(cells.subList(0, ISLAND_COUNT));
        OceanMap map = new OceanMap(DEFAULT_WIDTH, DEFAULT_HEIGHT, islands);
        List<Position> spawns = connectedSpawnCells(map, cells);
        if (spawns.size() < PIRATE_COUNT + 2) {
            throw new IllegalStateException("The ocean needs a connected region large enough for all spawns");
        }
        return new Game(map, spawns.getFirst(), spawns.subList(1, 1 + PIRATE_COUNT),
                spawns.get(1 + PIRATE_COUNT));
    }

    private static List<Position> connectedSpawnCells(OceanMap map, List<Position> cells) {
        Set<Position> visited = new HashSet<>();
        Set<Position> largestRegion = Set.of();
        for (Position cell : cells) {
            if (map.isNavigable(cell) && !visited.contains(cell)) {
                Set<Position> region = map.reachableFrom(cell);
                visited.addAll(region);
                if (region.size() > largestRegion.size()) {
                    largestRegion = region;
                }
            }
        }
        List<Position> spawns = new ArrayList<>();
        // Keep the seeded shuffle order rather than the unspecified iteration order of a set.
        for (Position cell : cells) {
            if (largestRegion.contains(cell)) {
                spawns.add(cell);
            }
        }
        return spawns;
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

    public Position getTreasurePosition() {
        return treasurePosition;
    }

    public GameState getState() {
        return state;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public boolean move(Direction direction) {
        return state.move(this, Objects.requireNonNull(direction));
    }

    /** Called by the playing state; pirate observers react before the turn's outcome is resolved. */
    boolean playTurn(Direction direction) {
        if (!player.move(direction)) {
            return false;
        }
        turnCount++;
        if (pirates.stream().anyMatch(pirate -> pirate.getPosition().equals(player.getPosition()))) {
            state = GameState.LOST;
        } else if (player.getPosition().equals(treasurePosition)) {
            state = GameState.WON;
        }
        return true;
    }
}
