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
import java.util.function.Function;

public final class ConfiguredAreaService implements MangoLocationApi {

    private volatile Snapshot snapshot = new Snapshot("world", "world_nether", 8.0,
            Map.of(), new ConcurrentHashMap<>(), List.of(), Area.synthetic("outskirts", "远郊", "world"),
            List.of(), Area.synthetic("outskirts", "远郊", "world_nether"), Map.of(), true,
            "&a你已进入 &e{area}", "&7你已离开 &e{area}");

    public ConfiguredAreaService() {
    }

    public int reload(FileConfiguration config) {
        String mainWorld = config.getString("main-world", "world");
        String netherWorld = config.getString("nether-mapping.world", "world_nether");
        double netherScale = config.getDouble("nether-mapping.coordinate-scale", 8.0);
        if (mainWorld.equals(netherWorld)) {
            throw new IllegalArgumentException("主世界和地狱世界名称不能相同");
        }
        if (!Double.isFinite(netherScale) || netherScale <= 0.0) {
            throw new IllegalArgumentException("nether-mapping.coordinate-scale 必须为正数");
        }
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
        for (String configuredId : root.getKeys(false)) {
            String id = configuredId.equals("interchange") ? "Hutong" : configuredId;
            if (!ids.add(id)) {
                throw new IllegalArgumentException("区域 ID 重复: " + id);
            }
            ConfigurationSection section = root.getConfigurationSection(configuredId);
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
        List<Area> netherAreas = loaded.stream()
                .map(area -> projectToNether(area, netherWorld, netherScale))
                .toList();
        Area netherFallback = Area.synthetic(outsideId, outsideName, netherWorld);
        Map<String, AreaMessages> messages = new LinkedHashMap<>();
        loaded.forEach(area -> messages.put(area.id(), loadAreaMessages(config, area)));
        messages.put(mainWorldFallback.id(), loadAreaMessages(config, mainWorldFallback));
        ConcurrentMap<String, Area> worldAreas = new ConcurrentHashMap<>();
        worldNames.forEach((worldName, displayName) ->
                worldAreas.put(worldName, Area.forWorld(worldName, displayName)));
        snapshot = new Snapshot(
                mainWorld,
                netherWorld,
                netherScale,
                Map.copyOf(worldNames),
                worldAreas,
                List.copyOf(loaded),
                mainWorldFallback,
                netherAreas,
                netherFallback,
                Map.copyOf(messages),
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
            if (current.netherWorld().equals(worldName)) {
                return current.netherAreas().stream()
                        .filter(area -> area.contains(worldName, x, z))
                        .findFirst()
                        .or(() -> Optional.of(current.netherFallback()));
            }
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

    public boolean isMainWorld(String worldName) {
        return snapshot.mainWorld().equals(worldName);
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

    public List<String> getEnterMessages(Area area) {
        return formatLines(area, AreaMessages::enter);
    }

    public List<String> getLeaveMessages(Area area) {
        return formatLines(area, AreaMessages::leave);
    }

    private List<String> formatLines(Area area, Function<AreaMessages, List<String>> selector) {
        AreaMessages messages = snapshot.areaMessages().getOrDefault(area.id(), defaultMessages(area));
        return selector.apply(messages).stream().map(line -> format(line, area)).toList();
    }

    private AreaMessages loadAreaMessages(FileConfiguration config, Area area) {
        AreaMessages defaults = defaultMessages(area);
        String base = "tracking.area-messages." + area.id();
        List<String> enter = config.getStringList(base + ".enter");
        List<String> leave = config.getStringList(base + ".leave");
        return new AreaMessages(enter.isEmpty() ? defaults.enter() : List.copyOf(enter),
                leave.isEmpty() ? defaults.leave() : List.copyOf(leave));
    }

    private AreaMessages defaultMessages(Area area) {
        return switch (area.id()) {
            case "san_cheng" -> new AreaMessages(
                    List.of("&6✦ 魅力三城，灯火如昼，欢迎来到三城区。", "&7三江风月入长街，一城烟火候君来。"),
                    List.of("&7你已离开三城区。", "&8回首长街灯未眠，且携清风赴前川。"));
            case "huadu" -> new AreaMessages(
                    List.of("&d✿ 魅力花都，芳华满城，欢迎来到花都区。", "&7花影随风铺锦路，满城春色待君游。"),
                    List.of("&7你已离开花都区。", "&8花香尚在衣襟上，前路清风亦有情。"));
            case "Hutong" -> new AreaMessages(
                    List.of("&b✦ 魅力互通，通衢八方，欢迎来到互通区。", "&7长路交汇连南北，一程风景共云天。"),
                    List.of("&7你已离开互通区。", "&8此去千程皆坦荡，愿君一路有清风。"));
            case "outskirts" -> new AreaMessages(
                    List.of("&a❧ 魅力远郊，山野舒展，欢迎来到远郊。", "&7远山含黛接晴野，陌上清风伴客行。"),
                    List.of("&7你已离开远郊。", "&8云影留痕辞旷野，灯火在前候归人。"));
            default -> new AreaMessages(
                    List.of("&6魅力{area}，风景正好，欢迎来到{area}。", "&7山水有意迎远客，清风一路伴君行。"),
                    List.of("&7你已离开{area}。", "&8且将此处风光记，前路相逢又一程。"));
        };
    }

    private String format(String template, Area area) {
        return template.replace("{area}", area.name()).replace("{id}", area.id());
    }

    private Area projectToNether(Area area, String netherWorld, double scale) {
        List<AreaPoint> projectedShape = area.shape().stream()
                .map(point -> new AreaPoint(point.x() / scale, point.z() / scale))
                .toList();
        return new Area(area.id(), area.name(), area.priority(), Set.of(netherWorld), projectedShape);
    }

    private record Snapshot(String mainWorld, String netherWorld, double netherScale,
                            Map<String, String> worldNames,
                            ConcurrentMap<String, Area> worldAreas, List<Area> areas,
                            Area mainWorldFallback, List<Area> netherAreas, Area netherFallback,
                            Map<String, AreaMessages> areaMessages,
                            boolean notifyPlayer, String enterMessage, String leaveMessage) {
    }

    private record AreaMessages(List<String> enter, List<String> leave) {
    }
}
