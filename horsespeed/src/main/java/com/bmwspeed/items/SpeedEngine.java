package com.bmwspeed.items;

import com.bmwspeed.BmwSpeed;
import com.bmwspeed.drive.LicensePlateManager;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpeedEngine {

    private final BmwSpeed plugin;
    private final NamespacedKey engineLevelKey;
    private final NamespacedKey engineColorKey;
    private final Map<Material, String> woolToEngineMap;
    private final Map<UUID, ScheduledTask> horseTasks;
    private final Map<UUID, Location> lastLocations;
    private final Map<UUID, Long> lastCollisionTimes;
    private LicensePlateManager licensePlateManager;

    public SpeedEngine(BmwSpeed plugin) {
        this.plugin = plugin;
        this.engineLevelKey = new NamespacedKey(plugin, "engine_level");
        this.engineColorKey = new NamespacedKey(plugin, "engine_color");
        this.woolToEngineMap = new HashMap<>();
        this.horseTasks = new ConcurrentHashMap<>();
        this.lastLocations = new ConcurrentHashMap<>();
        this.lastCollisionTimes = new ConcurrentHashMap<>();
        initializeWoolToEngineMap();
    }

    public void setLicensePlateManager(LicensePlateManager licensePlateManager) {
        this.licensePlateManager = licensePlateManager;
    }

    private void initializeWoolToEngineMap() {
        woolToEngineMap.put(Material.WHITE_WOOL, "白色车漆");
        woolToEngineMap.put(Material.ORANGE_WOOL, "橙色车漆");
        woolToEngineMap.put(Material.MAGENTA_WOOL, "品红色车漆");
        woolToEngineMap.put(Material.LIGHT_BLUE_WOOL, "浅蓝色车漆");
        woolToEngineMap.put(Material.YELLOW_WOOL, "黄色车漆");
        woolToEngineMap.put(Material.LIME_WOOL, "青柠色车漆");
        woolToEngineMap.put(Material.PINK_WOOL, "粉色车漆");
        woolToEngineMap.put(Material.GRAY_WOOL, "灰色车漆");
        woolToEngineMap.put(Material.LIGHT_GRAY_WOOL, "浅灰色车漆");
        woolToEngineMap.put(Material.CYAN_WOOL, "青色车漆");
        woolToEngineMap.put(Material.PURPLE_WOOL, "紫色车漆");
        woolToEngineMap.put(Material.BLUE_WOOL, "蓝色车漆");
        woolToEngineMap.put(Material.BROWN_WOOL, "棕色车漆");
        woolToEngineMap.put(Material.GREEN_WOOL, "绿色车漆");
        woolToEngineMap.put(Material.RED_WOOL, "红色车漆");
        woolToEngineMap.put(Material.BLACK_WOOL, "黑色车漆");
    }

    public ItemStack createSpeedEngine(int level, String colorName, Material woolMaterial) {
        ItemStack item = new ItemStack(Material.SADDLE);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String nameFormat = plugin.getRuntimeConfig().getString("item-names.speed-engine", "&b%s 跑车引擎 %d");
            String displayName = String.format(nameFormat, colorName, level);
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName)
                    .decoration(TextDecoration.ITALIC, false));

            List<String> loreStrings = plugin.getRuntimeConfig().getStringList("item-names.lore");
            List<Component> lore = new ArrayList<>();
            for (String loreLine : loreStrings) {
                String formattedLine = String.format(loreLine, level, level);
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(formattedLine)
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);

            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(engineLevelKey, PersistentDataType.INTEGER, level);
            data.set(engineColorKey, PersistentDataType.STRING, colorName);

            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isSpeedEngine(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        if (item.getType() != Material.SADDLE) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(engineLevelKey, PersistentDataType.INTEGER);
    }

    public int getEngineLevel(ItemStack item) {
        if (!isSpeedEngine(item)) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.getOrDefault(engineLevelKey, PersistentDataType.INTEGER, 0);
    }

    public double getSpeedValue(int level) {
        return plugin.getRuntimeConfig().getDouble("speed-levels." + level, 0.2);
    }

    public NamespacedKey getEngineLevelKey() {
        return engineLevelKey;
    }

    public NamespacedKey getEngineColorKey() {
        return engineColorKey;
    }

    public void applySpeedEffect(AbstractHorse horse, ItemStack engine) {
        if (engine == null) {
            return;
        }

        int level = getEngineLevel(engine);
        if (level <= 0) {
            return;
        }

        double speedBoost = getSpeedValue(level);
        if (speedBoost <= 0) {
            return;
        }

        AttributeInstance attribute = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);

        if (attribute == null) {
            return;
        }

        NamespacedKey modifierKey = new NamespacedKey(plugin, "bmwspeed_boost");
        attribute.removeModifier(modifierKey);

        AttributeModifier modifier = new AttributeModifier(
                modifierKey,
                speedBoost,
                AttributeModifier.Operation.ADD_NUMBER
        );
        attribute.addModifier(modifier);

        UUID horseId = horse.getUniqueId();

        ScheduledTask existingTask = horseTasks.get(horseId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        long interval = Math.max(2L, plugin.getRuntimeConfig().getLong("collision-check-interval-ticks", 4L));
        ScheduledTask task = horse.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (!horse.isValid() || horse.getPassengers().isEmpty()) {
                removeSpeedEffect(horse);
                scheduledTask.cancel();
                horseTasks.remove(horseId);
                return;
            }

            ItemStack equippedEngine = getEquippedEngine(horse);
            if (equippedEngine == null || !isSpeedEngine(equippedEngine)) {
                removeSpeedEffect(horse);
                return;
            }

            Location currentLoc = horse.getLocation();
            Location lastLoc = lastLocations.get(horseId);

            if (lastLoc != null && lastLoc.getWorld().equals(currentLoc.getWorld())) {
                double dist = Math.sqrt(Math.pow(currentLoc.getX() - lastLoc.getX(), 2) + Math.pow(currentLoc.getZ() - lastLoc.getZ(), 2));

                if (dist > 0.15) {
                    org.bukkit.util.Vector horizontalDir = currentLoc.getDirection();
                    horizontalDir.setY(0);
                    if (horizontalDir.lengthSquared() > 0) {
                        horizontalDir.normalize();
                    }

                    Location checkLoc = currentLoc.clone().add(horizontalDir.multiply(2.8)).add(0, 0.5, 0);

                    if (checkLoc.getBlock().getType().isSolid()) {
                        long now = System.currentTimeMillis();
                        long lastHit = lastCollisionTimes.getOrDefault(horseId, 0L);

                        if (now - lastHit > 1000L) {
                            lastCollisionTimes.put(horseId, now);

                            double damage = Math.min(4.0, dist * 15.0);

                            if (horse.getHealth() <= damage) {
                                org.bukkit.entity.Player player = (org.bukkit.entity.Player) horse.getPassengers().get(0);
                                String plate = licensePlateManager != null ? licensePlateManager.getPlate(horse) : null;
                                int engineLevel = getEngineLevel(equippedEngine);
                                String carType = plugin.getDriveSystemManager().getCarTypeName(engineLevel);

                                if (horse.getInventory() != null) {
                                    horse.getInventory().setSaddle(null);
                                    horse.getInventory().clear();
                                }
                                horse.remove();

                                boolean damageBlocks = plugin.getRuntimeConfig().getBoolean("collision-explosion-damages-blocks", false);
                                currentLoc.getWorld().createExplosion(currentLoc, 4.0f, false, damageBlocks);

                                player.setHealth(0);
                                String safePlate = (plate != null) ? plate : "未知";
                                player.sendMessage(ChatColor.RED + "💥 您的" + carType + " " + safePlate + "因撞墙而车毁人亡！");
                                if (licensePlateManager != null) {
                                    licensePlateManager.logCrash(player, horse, "撞墙");
                                }

                                removeSpeedEffect(horse);
                                scheduledTask.cancel();
                                return;
                            } else {
                                horse.damage(damage);
                                horse.getWorld().playSound(currentLoc, Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.0f, 1.0f);
                            }
                        }
                    }
                }
            }
            lastLocations.put(horseId, currentLoc);

            if (horse.getHealth() <= 5.0) {
                horse.getWorld().spawnParticle(org.bukkit.Particle.CAMPFIRE_COSY_SMOKE, currentLoc.clone().add(0, 2, 0), 5, 0.5, 0.5, 0.5, 0.05);
            }

        }, null, interval, interval);

        horseTasks.put(horseId, task);
    }

    public void removeSpeedEffect(AbstractHorse horse) {
        AttributeInstance attribute = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);

        if (attribute == null) {
            return;
        }

        NamespacedKey modifierKey = new NamespacedKey(plugin, "bmwspeed_boost");
        attribute.removeModifier(modifierKey);

        UUID horseId = horse.getUniqueId();
        ScheduledTask task = horseTasks.remove(horseId);
        if (task != null) {
            task.cancel();
        }
        lastLocations.remove(horseId);
        lastCollisionTimes.remove(horseId);
    }

    private ItemStack getEquippedEngine(AbstractHorse horse) {
        if (horse.getInventory() == null) {
            return null;
        }
        ItemStack item = horse.getInventory().getSaddle();
        if (item != null && item.getType() == Material.SADDLE) {
            return item;
        }
        return null;
    }
}
