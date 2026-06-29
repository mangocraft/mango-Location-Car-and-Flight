package com.mangolocation.api;

import java.util.List;
import java.util.Set;

/** Immutable description of a configured geographic area. */
public record Area(String id, String name, int priority, Set<String> worlds, List<AreaPoint> shape) {

    private static final double EPSILON = 1.0E-7;

    public Area {
        worlds = Set.copyOf(worlds);
        shape = List.copyOf(shape);
        if (!shape.isEmpty() && shape.size() < 3) {
            throw new IllegalArgumentException("An area requires either zero or at least three points");
        }
    }

    /** Creates a synthetic area representing a non-main world/dimension. */
    public static Area forWorld(String worldName, String displayName) {
        return synthetic("world:" + worldName, displayName, worldName);
    }

    public boolean isWorldArea() {
        return isSyntheticArea() && id.startsWith("world:");
    }

    /** Creates a non-polygon fallback area for all coordinates in one world. */
    public static Area synthetic(String id, String displayName, String worldName) {
        return new Area(id, displayName, Integer.MAX_VALUE, Set.of(worldName), List.of());
    }

    public boolean isSyntheticArea() {
        return shape.isEmpty();
    }

    /** Empty worlds means this area is enabled in every world. */
    public boolean supportsWorld(String worldName) {
        return worlds.isEmpty() || worlds.contains(worldName);
    }

    /** Includes points that lie exactly on a polygon edge. */
    public boolean contains(String worldName, double x, double z) {
        if (!supportsWorld(worldName)) {
            return false;
        }
        if (isSyntheticArea()) {
            return true;
        }

        boolean inside = false;
        for (int i = 0, j = shape.size() - 1; i < shape.size(); j = i++) {
            AreaPoint a = shape.get(j);
            AreaPoint b = shape.get(i);
            if (isOnSegment(a, b, x, z)) {
                return true;
            }

            boolean crosses = (b.z() > z) != (a.z() > z);
            if (crosses && x < (a.x() - b.x()) * (z - b.z()) / (a.z() - b.z()) + b.x()) {
                inside = !inside;
            }
        }
        return inside;
    }

    private boolean isOnSegment(AreaPoint a, AreaPoint b, double x, double z) {
        double cross = (x - a.x()) * (b.z() - a.z()) - (z - a.z()) * (b.x() - a.x());
        if (Math.abs(cross) > EPSILON) {
            return false;
        }
        return x >= Math.min(a.x(), b.x()) - EPSILON
                && x <= Math.max(a.x(), b.x()) + EPSILON
                && z >= Math.min(a.z(), b.z()) - EPSILON
                && z <= Math.max(a.z(), b.z()) + EPSILON;
    }
}
