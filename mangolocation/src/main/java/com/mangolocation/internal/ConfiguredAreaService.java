package com.mangolocation.internal;

import com.mangolocation.api.Area;
import com.mangolocation.api.AreaPoint;
import com.mangolocation.api.MangoLocationApi;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ConfiguredAreaService implements MangoLocationApi {

    private volatile Snapshot snapshot = new Snapshot("world", Map.of(), new ConcurrentHashMap<>(), List.of(),
            Area.synthetic("outskirts", "远郊", "world"), true,
            "&a你已进入 &e{area}", "&7你已离开 &e{area}");

    public ConfiguredAreaService() {
    }

    public int reload(FileConfiguration config) {
        String mainWorld = config.getString("main-world", "world");
        String outsideId = config.getString("main-world-outside-area.id", "outskirts");
        String outsideName = config.getString("main-world-outside-area.name", "远郊");
        Map<String, String> worldNames = new LinkedHashMap<>();
        worldNames.put("world_nether", "地狱");
        worldNames.put("world_the_end", "末地");
        ConfigurationSection worldNamesSection = config.getConfigurationSection("world-display-names");
        if (worldNamesSection != null) {
            for (String worldName : worldNamesSection.getKeys(false)) {
                worldNames.put(worldName, worldNamesSection.getString(worldName, worldName));
            }
        }

        ConfigurationSection root = config.getConfigurationSection("areas");
        if (root == null) {
            throw new IllegalArgumentException("config.yml 缺少 areas 配置段");
        }

        List<Area> loaded = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (String id : root.getKeys(false)) {
            if (!ids.add(id)) {
                throw new IllegalArgumentException("区域 ID 重复: " + id);
            }
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null || !section.getBoolean("enabled", true)) {
                continue;
            }

            String name = section.getString("name", id);
            int priority = section.getInt("priority", 100);
            List<?> rawShape = section.getList("shape", List.of());
            List<AreaPoint> points = new ArrayList<>();
            for (Object value : rawShape) {
                if (!(value instanceof java.util.Map<?, ?> point)) {
                    throw new IllegalArgumentException("区域 " + id + " 包含无效坐标");
                }
                Object rawX = point.get("x");
                Object rawZ = point.get("z");
                if (!(rawX instanceof Number x) || !(rawZ instanceof Number z)) {
                    throw new IllegalArgumentException("区域 " + id + " 的坐标必须包含数字 x/z");
                }
                points.add(new AreaPoint(x.doubleValue(), z.doubleValue()));
            }
            loaded.add(new Area(id, name, priority, Set.of(mainWorld), points));
        }

        loaded.sort(Comparator.comparingInt(Area::priority));
        if (ids.contains(outsideId)) {
            throw new IllegalArgumentException("远郊兜底区域 ID 与多边形区域重复: " + outsideId);
        }
        Area mainWorldFallback = Area.synthetic(outsideId, outsideName, mainWorld);
        ConcurrentMap<String, Area> worldAreas = new ConcurrentHashMap<>();
        worldNames.forEach((worldName, displayName) ->
                worldAreas.put(worldName, Area.forWorld(worldName, displayName)));
        snapshot = new Snapshot(
                mainWorld,
                Map.copyOf(worldNames),
                worldAreas,
                List.copyOf(loaded),
                mainWorldFallback,
                config.getBoolean("tracking.notify-player", true),
                config.getString("tracking.enter-message", "&a你已进入 &e{area}"),
                config.getString("tracking.leave-message", "&7你已离开 &e{area}")
        );
        return loaded.size();
    }

    @Override
    public Optional<Area> findArea(String worldName, double x, double z) {
        Snapshot current = snapshot;
        if (!current.mainWorld().equals(worldName)) {
            return Optional.of(current.worldAreas().computeIfAbsent(worldName,
                    key -> Area.forWorld(key, current.worldNames().getOrDefault(key, key))));
        }
        return current.areas().stream()
                .filter(area -> area.contains(worldName, x, z))
                .findFirst()
                .or(() -> Optional.of(current.mainWorldFallback()));
    }

    @Override
    public List<Area> getAreas() {
        return snapshot.areas();
    }

    public boolean shouldNotifyPlayer() {
        return snapshot.notifyPlayer();
    }

    public String formatEnterMessage(Area area) {
        return format(snapshot.enterMessage(), area);
    }

    public String formatLeaveMessage(Area area) {
        return format(snapshot.leaveMessage(), area);
    }

    private String format(String template, Area area) {
        return template.replace("{area}", area.name()).replace("{id}", area.id());
    }

    private record Snapshot(String mainWorld, Map<String, String> worldNames,
                            ConcurrentMap<String, Area> worldAreas, List<Area> areas,
                            Area mainWorldFallback,
                            boolean notifyPlayer, String enterMessage, String leaveMessage) {
    }
}
