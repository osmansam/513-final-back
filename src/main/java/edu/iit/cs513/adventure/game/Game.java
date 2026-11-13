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
    private static final List<PirateShipFactory> PIRATE_FACTORIES =
            List.of(new HunterPirateFactory(), new InterceptorPirateFactory());
    private static final int MONSTER_COUNT = 2;

    private final OceanMap oceanMap;
    private final Ship player;
    private final List<PirateShip> pirates;
    private final List<SeaMonster> monsters;
    private final Position treasurePosition;
    private GameState state = GameState.PLAYING;
    private int turnCount;

    /** Builds a layout with distinct spawns and treasure reachable from the player. */
    public Game(OceanMap oceanMap, Position playerStart, List<Position> pirateStarts, Position treasurePosition) {
        this(oceanMap, playerStart, pirateStarts, treasurePosition, List.of());
    }

    public Game(OceanMap oceanMap, Position playerStart, List<Position> pirateStarts,
                Position treasurePosition, List<MonsterSpawn> monsterSpawns) {
        this(oceanMap, playerStart, treasurePosition,
                pirateStarts.stream().map(start -> new PirateShip(start, oceanMap)).toList(), monsterSpawns);
    }

    private Game(OceanMap oceanMap, Position playerStart, Position treasurePosition,
                 List<PirateShip> ships, List<MonsterSpawn> monsterSpawns) {
        this.oceanMap = Objects.requireNonNull(oceanMap);
        this.treasurePosition = treasurePosition;
        player = new Ship(playerStart, oceanMap);
        Set<Position> occupied = new HashSet<>();
        occupied.add(playerStart);
        for (PirateShip ship : ships) {
            if (!occupied.add(ship.getPosition())) {
                throw new IllegalArgumentException("Ships must start on distinct cells");
            }
        }
        pirates = List.copyOf(ships);
        if (!oceanMap.isNavigable(treasurePosition) || occupied.contains(treasurePosition)) {
            throw new IllegalArgumentException("Treasure must start on a water cell separate from all ships");
        }
        if (!oceanMap.reachableFrom(playerStart).contains(treasurePosition)) {
            throw new IllegalArgumentException("Treasure must be reachable from the player's starting cell");
        }
        occupied.add(treasurePosition);
        List<SeaMonster> creatures = new ArrayList<>();
        for (MonsterSpawn spawn : monsterSpawns) {
            if (!occupied.add(spawn.position())) {
                throw new IllegalArgumentException("Monsters must start separate from ships, treasure, and other monsters");
            }
            creatures.add(new SeaMonster(spawn.position(), oceanMap, spawn.area()));
        }
        monsters = List.copyOf(creatures);
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
        int pirateCount = PIRATE_FACTORIES.size();
        if (spawns.size() < pirateCount + MONSTER_COUNT + 2) {
            throw new IllegalStateException("The ocean needs a connected region large enough for all spawns");
        }
        List<PirateShip> pirates = new ArrayList<>();
        for (int index = 0; index < pirateCount; index++) {
            pirates.add(PIRATE_FACTORIES.get(index).create(spawns.get(1 + index), map));
        }
        List<MonsterSpawn> monsters = new ArrayList<>();
        for (int index = 0; index < MONSTER_COUNT; index++) {
            Position start = spawns.get(pirateCount + 2 + index);
            RoamingArea area = new RoamingArea(Math.max(0, start.x() - 2), Math.max(0, start.y() - 2),
                    Math.min(map.getWidth() - 1, start.x() + 2), Math.min(map.getHeight() - 1, start.y() + 2));
            monsters.add(new MonsterSpawn(start, area));
        }
        return new Game(map, spawns.getFirst(), spawns.get(pirateCount + 1), pirates, monsters);
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

    public List<Position> getMonsterPositions() {
        return monsters.stream().map(SeaMonster::getPosition).toList();
    }

    public List<RoamingArea> getMonsterAreas() {
        return monsters.stream().map(SeaMonster::getArea).toList();
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
        for (SeaMonster monster : monsters) {
            // An existing collision must not be undone by a monster moving away.
            if (!monster.getPosition().equals(player.getPosition())) {
                monster.move();
            }
        }
        if (pirates.stream().anyMatch(pirate -> pirate.getPosition().equals(player.getPosition()))
                || monsters.stream().anyMatch(monster -> monster.getPosition().equals(player.getPosition()))) {
            state = GameState.LOST;
        } else if (player.getPosition().equals(treasurePosition)) {
            state = GameState.WON;
        }
        return true;
    }
}
