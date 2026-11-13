package edu.iit.cs513.adventure.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OceanMapTest {
    @Test
    void rectangularMapUsesItsOwnWidthAndHeight() {
        OceanMap map = new OceanMap(7, 3, Set.of(new Position(5, 1)));

        assertTrue(map.isNavigable(new Position(6, 2)));
        assertTrue(map.contains(new Position(0, 0)));
        assertFalse(map.contains(new Position(7, 2)));
        assertFalse(map.contains(new Position(6, 3)));
        assertFalse(map.contains(new Position(-1, 0)));
        assertFalse(map.contains(new Position(0, -1)));
        assertTrue(map.isIsland(new Position(5, 1)));
        assertFalse(map.isNavigable(new Position(5, 1)));
        assertFalse(map.isNavigable(new Position(7, 2)));
    }

    @Test
    void terrainCannotBeChangedThroughConstructorInputOrGetter() {
        Set<Position> islands = new HashSet<>(Set.of(new Position(1, 1)));
        OceanMap map = new OceanMap(3, 3, islands);
        islands.clear();

        assertTrue(map.isIsland(new Position(1, 1)));
        assertThrows(UnsupportedOperationException.class, () -> map.getIslands().clear());
    }

    @ParameterizedTest
    @CsvSource({"0,3", "3,0", "-1,3", "3,-1"})
    void rejectsNonPositiveDimensions(int width, int height) {
        assertThrows(IllegalArgumentException.class, () -> new OceanMap(width, height, Set.of()));
    }

    @ParameterizedTest
    @CsvSource({"-1,0", "0,-1", "3,0", "0,3"})
    void rejectsIslandsOutsideMap(int x, int y) {
        assertThrows(IllegalArgumentException.class,
                () -> new OceanMap(3, 3, Set.of(new Position(x, y))));
    }
}
