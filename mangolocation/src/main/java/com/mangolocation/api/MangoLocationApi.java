package com.mangolocation.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/** Public service exposed through Bukkit's ServicesManager. */
public interface MangoLocationApi {

    /**
     * Thread-safe coordinate-only lookup. The configured main world returns an administrative
     * polygon or empty; every other world returns a synthetic world area.
     */
    Optional<Area> findArea(String worldName, double x, double z);

    default Optional<Area> findArea(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        return findArea(location.getWorld().getName(), location.getX(), location.getZ());
    }

    default Optional<Area> findArea(Player player) {
        return findArea(player.getLocation());
    }

    default boolean isInArea(Location location, String areaId) {
        return findArea(location).map(area -> area.id().equals(areaId)).orElse(false);
    }

    List<Area> getAreas();
}
