package com.mangolocation.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaTest {

    @Test
    void preservesTheConcaveHuaduShape() {
        Area huadu = area("huadu", List.of(
                new AreaPoint(5348, 4892),
                new AreaPoint(5348, -4359),
                new AreaPoint(17348, -4359),
                new AreaPoint(17348, 14080),
                new AreaPoint(10032, 14080),
                new AreaPoint(10032, 4892)
        ));

        assertTrue(huadu.contains("world", 8000, 0));
        assertTrue(huadu.contains("world", 12000, 10000));
        assertFalse(huadu.contains("world", 8000, 10000));
    }

    @Test
    void includesPolygonEdgesAndHonorsWorldFiltering() {
        Area interchange = new Area("interchange", "互通区", 10, Set.of("world"), List.of(
                new AreaPoint(5348, 4892),
                new AreaPoint(-2968, 4892),
                new AreaPoint(-2968, -4368),
                new AreaPoint(5349, -4359)
        ));

        assertTrue(interchange.contains("world", 5348, 4892));
        assertTrue(interchange.contains("world", 0, 0));
        assertFalse(interchange.contains("world_nether", 0, 0));
    }

    @Test
    void supportsSyntheticWorldAreas() {
        Area end = Area.forWorld("world_the_end", "末地");

        assertTrue(end.isWorldArea());
        assertTrue(end.contains("world_the_end", 100_000, -100_000));
        assertFalse(end.contains("world", 0, 0));
    }

    @Test
    void keepsTheOriginalSlopedSanChengBoundary() {
        Area sanCheng = area("san_cheng", List.of(
                new AreaPoint(10032, 14080),
                new AreaPoint(-3120, 13888),
                new AreaPoint(-2968, 4892),
                new AreaPoint(10032, 4892)
        ));

        assertTrue(sanCheng.contains("world", 0, 10000));
        assertFalse(sanCheng.contains("world", -3120, 10000));
    }

    private Area area(String id, List<AreaPoint> points) {
        return new Area(id, id, 10, Set.of(), points);
    }
}
