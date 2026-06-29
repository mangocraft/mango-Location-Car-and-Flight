package com.bmwspeed.drive;

import com.bmwspeed.BmwSpeed;
import com.mangolocation.api.MangoLocationApi;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractHorse;
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

public class LicensePlateManager {
    private final BmwSpeed plugin;
    private final NamespacedKey LICENSE_PLATE_KEY;
    private final MangoLocationApi locationApi;
    private File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, String> plateCache;
    private final Set<String> usedPlates;
    private final AsyncPlateStore plateStore;

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public LicensePlateManager(BmwSpeed plugin) {
        this.plugin = plugin;
        this.LICENSE_PLATE_KEY = new NamespacedKey(plugin, "license_plate");
        this.locationApi = plugin.getServer().getServicesManager().load(MangoLocationApi.class);
        if (locationApi == null) {
            throw new IllegalStateException("未找到 MangoLocation API 服务");
        }
        this.plateCache = new ConcurrentHashMap<>();
        this.usedPlates = ConcurrentHashMap.newKeySet();
        loadDataFile();
        this.plateStore = new AsyncPlateStore(plugin, dataFile, () -> plateCache);
    }

    private void loadDataFile() {
        dataFile = new File(plugin.getDataFolder(), "plates.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建 plates.yml 文件：" + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadCache();
    }

    private void loadCache() {
        for (String key : dataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String plate = dataConfig.getString(key);
                if (plate != null) {
                    plateCache.put(uuid, plate);
                    usedPlates.add(plate);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("plates.yml 中发现无效的 UUID: " + key);
            }
        }
    }

    private String generatePlate(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 5; i++) {
            sb.append(CHARS.charAt(ThreadLocalRandom.current().nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String getUniquePlate(String prefix) {
        String plate;
        do {
            plate = generatePlate(prefix);
        } while (!usedPlates.add(plate));
        return plate;
    }

    private String getPrefix(AbstractHorse horse) {
        String fallback = plugin.getRuntimeConfig().getString("registration.default-prefix", "芒 A-");
        return locationApi.findArea(horse.getLocation())
                .map(area -> plugin.getRuntimeConfig().getString("registration.area-prefixes." + area.id(), fallback))
                .orElse(fallback);
    }

    private String getCarType(int level) {
        switch (level) {
            case 6:
                return "宝马 M8";
            case 5:
                return "宝马 M5";
            case 4:
                return "宝马 7 系";
            case 3:
                return "宝马 5 系";
            case 2:
                return "宝马 3 系";
            case 1:
                return "宝马 1 系";
            default:
                return "未知车型";
        }
    }

    public String getPlate(AbstractHorse horse) {
        UUID uuid = horse.getUniqueId();

        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        if (pdc.has(LICENSE_PLATE_KEY, PersistentDataType.STRING)) {
            String plate = pdc.get(LICENSE_PLATE_KEY, PersistentDataType.STRING);
            plateCache.put(uuid, plate);
            usedPlates.add(plate);
            return plate;
        }

        String plate = plateCache.get(uuid);
        if (plate != null) {
            pdc.set(LICENSE_PLATE_KEY, PersistentDataType.STRING, plate);
            return plate;
        }

        return null;
    }

    public String assignPlate(AbstractHorse horse, int level) {
        String plate = getPlate(horse);
        if (plate == null) {
            plate = getUniquePlate(getPrefix(horse));
            UUID uuid = horse.getUniqueId();

            PersistentDataContainer pdc = horse.getPersistentDataContainer();
            pdc.set(LICENSE_PLATE_KEY, PersistentDataType.STRING, plate);

            plateCache.put(uuid, plate);
            plateStore.requestSave();
        }

        String carType = plugin.getDriveSystemManager().getCarTypeName(level);
        horse.setCustomName("§b" + plate + " " + carType);
        horse.setCustomNameVisible(true);

        return plate;
    }

    public void shutdown() {
        plateStore.close();
    }

    public boolean hasPlate(AbstractHorse horse) {
        return getPlate(horse) != null;
    }

    public void logDriveStart(Player player, AbstractHorse vehicle) {
        String plate = getPlate(vehicle);
        if (plate != null) {
            plugin.getLogger().info("[BmwSpeed] " + plate + "号宝马跑车正在被 " + player.getName() + " 驾驶");
        }
    }

    public void logCrash(Player player, AbstractHorse vehicle, String reason) {
        String plate = getPlate(vehicle);
        if (plate != null) {
            String message = "⚠️ 玩家 " + player.getName() + " 在驾驶 " + plate + " 因 " + reason + " 导致车毁人亡";
            plugin.getLogger().warning(message);
            player.getServer().broadcastMessage(message);
        }
    }
}
