package com.ghastspeed.flight;

import com.ghastspeed.GhostSpeed;
import com.mangolocation.api.MangoLocationApi;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Set;

public class TailNumberManager {
    private final GhostSpeed plugin;
    private final NamespacedKey TAIL_NUMBER_KEY;
    private final MangoLocationApi locationApi;
    private File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, String> tailNumberCache;
    private final Set<String> usedTailNumbers;
    private final AsyncTailNumberStore tailNumberStore;

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public TailNumberManager(GhostSpeed plugin) {
        this.plugin = plugin;
        this.TAIL_NUMBER_KEY = new NamespacedKey(plugin, "tail_number");
        this.locationApi = plugin.getServer().getServicesManager().load(MangoLocationApi.class);
        if (locationApi == null) {
            throw new IllegalStateException("未找到 MangoLocation API 服务");
        }
        this.tailNumberCache = new ConcurrentHashMap<>();
        this.usedTailNumbers = ConcurrentHashMap.newKeySet();
        loadDataFile();
        this.tailNumberStore = new AsyncTailNumberStore(plugin, dataFile, () -> tailNumberCache);
    }

    private void loadDataFile() {
        dataFile = new File(plugin.getDataFolder(), "jijia.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建jijia.yml文件: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadCache();
    }

    private void loadCache() {
        for (String key : dataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String tailNumber = dataConfig.getString(key);
                if (tailNumber != null) {
                    tailNumberCache.put(uuid, tailNumber);
                    usedTailNumbers.add(tailNumber);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("jijia.yml中发现无效的UUID: " + key);
            }
        }
    }

    private String generateTailNumber(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 4; i++) {
            sb.append(CHARS.charAt(ThreadLocalRandom.current().nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String getUniqueTailNumber(String prefix) {
        String tailNumber;
        do {
            tailNumber = generateTailNumber(prefix);
        } while (!usedTailNumbers.add(tailNumber));
        return tailNumber;
    }

    private String getPrefix(LivingEntity entity) {
        String fallback = plugin.getRuntimeConfig().getString("registration.default-prefix", "B-");
        return locationApi.findArea(entity.getLocation())
                .map(area -> {
                    String areaDefault = switch (area.id()) {
                        case "Hutong" -> "B-I";
                        case "outskirts" -> "B-O";
                        default -> fallback;
                    };
                    return plugin.getRuntimeConfig().getString(
                            "registration.area-prefixes." + area.id(), areaDefault);
                })
                .orElse(fallback);
    }

    private String getPlaneType(int level) {
        switch (level) {
            case 6:
                return "干线客机";
            case 5:
                return "支线客机";
            case 4:
                return "小型客机4型";
            case 3:
                return "小型客机3型";
            case 2:
                return "小型客机2型";
            case 1:
                return "小型客机1型";
            default:
                return "未知机型";
        }
    }

    public String getTailNumber(LivingEntity entity) {
        UUID uuid = entity.getUniqueId();

        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (pdc.has(TAIL_NUMBER_KEY, PersistentDataType.STRING)) {
            String tailNumber = pdc.get(TAIL_NUMBER_KEY, PersistentDataType.STRING);
            tailNumberCache.put(uuid, tailNumber);
            usedTailNumbers.add(tailNumber);
            return tailNumber;
        }

        String tailNumber = tailNumberCache.get(uuid);
        if (tailNumber != null) {
            pdc.set(TAIL_NUMBER_KEY, PersistentDataType.STRING, tailNumber);
            return tailNumber;
        }

        return null;
    }

    public String assignTailNumber(LivingEntity entity, int level) {
        String tailNumber = getTailNumber(entity);
        if (tailNumber == null) {
            tailNumber = getUniqueTailNumber(getPrefix(entity));
            UUID uuid = entity.getUniqueId();

            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            pdc.set(TAIL_NUMBER_KEY, PersistentDataType.STRING, tailNumber);

            tailNumberCache.put(uuid, tailNumber);
            tailNumberStore.requestSave();
        }

        String planeType = plugin.getFlightSystemManager().getPlaneTypeName(level);
        entity.setCustomName("§b" + tailNumber + " " + planeType);
        entity.setCustomNameVisible(true);

        return tailNumber;
    }

    public void shutdown() {
        tailNumberStore.close();
    }

    public boolean hasTailNumber(LivingEntity entity) {
        return getTailNumber(entity) != null;
    }

    public void logFlightTakeoff(Player player, LivingEntity vehicle) {
        String tailNumber = getTailNumber(vehicle);
        if (tailNumber != null) {
            plugin.getLogger().info("[GhostSpeed] " + tailNumber + "号快乐恶魂正在被 " + player.getName() + " 骑乘");
        }
    }

    public void logCrash(Player player, LivingEntity vehicle, String reason) {
        String tailNumber = getTailNumber(vehicle);
        if (tailNumber != null) {
            String message = "⚠️ 玩家 " + player.getName() + " 在乘坐 " + tailNumber + " 因 " + reason + " 导致坠机爆炸";
            plugin.getLogger().warning(message);
            player.getServer().broadcastMessage(message);
        }
    }
}
