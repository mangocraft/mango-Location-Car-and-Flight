package com.ghastspeed.items;

import com.ghastspeed.GhostSpeed;
import com.ghastspeed.flight.TailNumberManager;
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
import org.bukkit.entity.LivingEntity;
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

public class SpeedHarness {

    private final GhostSpeed plugin;
    private final NamespacedKey speedLevelKey;
    private final NamespacedKey harnessColorKey;
    private final Map<Material, Material> woolToHarnessMap;
    private final Map<UUID, ScheduledTask> entityTasks;
    private final Map<UUID, Location> lastLocations;
    private final Map<UUID, Long> lastCollisionTimes;
    private TailNumberManager tailNumberManager;

    public SpeedHarness(GhostSpeed plugin) {
        this.plugin = plugin;
        this.speedLevelKey = new NamespacedKey(plugin, "speed_level");
        this.harnessColorKey = new NamespacedKey(plugin, "harness_color");
        this.woolToHarnessMap = new HashMap<>();
        this.entityTasks = new ConcurrentHashMap<>();
        this.lastLocations = new ConcurrentHashMap<>();
        this.lastCollisionTimes = new ConcurrentHashMap<>();
        initializeWoolToHarnessMap();
    }

    public void setTailNumberManager(TailNumberManager tailNumberManager) {
        this.tailNumberManager = tailNumberManager;
    }

    private void initializeWoolToHarnessMap() {
        woolToHarnessMap.put(Material.WHITE_WOOL, Material.valueOf("WHITE_HARNESS"));
        woolToHarnessMap.put(Material.ORANGE_WOOL, Material.valueOf("ORANGE_HARNESS"));
        woolToHarnessMap.put(Material.MAGENTA_WOOL, Material.valueOf("MAGENTA_HARNESS"));
        woolToHarnessMap.put(Material.LIGHT_BLUE_WOOL, Material.valueOf("LIGHT_BLUE_HARNESS"));
        woolToHarnessMap.put(Material.YELLOW_WOOL, Material.valueOf("YELLOW_HARNESS"));
        woolToHarnessMap.put(Material.LIME_WOOL, Material.valueOf("LIME_HARNESS"));
        woolToHarnessMap.put(Material.PINK_WOOL, Material.valueOf("PINK_HARNESS"));
        woolToHarnessMap.put(Material.GRAY_WOOL, Material.valueOf("GRAY_HARNESS"));
        woolToHarnessMap.put(Material.LIGHT_GRAY_WOOL, Material.valueOf("LIGHT_GRAY_HARNESS"));
        woolToHarnessMap.put(Material.CYAN_WOOL, Material.valueOf("CYAN_HARNESS"));
        woolToHarnessMap.put(Material.PURPLE_WOOL, Material.valueOf("PURPLE_HARNESS"));
        woolToHarnessMap.put(Material.BLUE_WOOL, Material.valueOf("BLUE_HARNESS"));
        woolToHarnessMap.put(Material.BROWN_WOOL, Material.valueOf("BROWN_HARNESS"));
        woolToHarnessMap.put(Material.GREEN_WOOL, Material.valueOf("GREEN_HARNESS"));
        woolToHarnessMap.put(Material.RED_WOOL, Material.valueOf("RED_HARNESS"));
        woolToHarnessMap.put(Material.BLACK_WOOL, Material.valueOf("BLACK_HARNESS"));
    }

    public ItemStack createSpeedHarness(int level, String colorName, Material woolMaterial) {
        Material harnessMaterial = woolToHarnessMap.getOrDefault(woolMaterial, Material.valueOf("WHITE_HARNESS"));
        ItemStack item = new ItemStack(harnessMaterial);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String nameFormat = plugin.getRuntimeConfig().getString("item-names.speed-harness", "&b%s 速度护具 %d");
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
            data.set(speedLevelKey, PersistentDataType.INTEGER, level);
            data.set(harnessColorKey, PersistentDataType.STRING, colorName);

            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isSpeedHarness(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(speedLevelKey, PersistentDataType.INTEGER);
    }

    public int getSpeedLevel(ItemStack item) {
        if (!isSpeedHarness(item)) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.getOrDefault(speedLevelKey, PersistentDataType.INTEGER, 0);
    }

    public double getSpeedValue(int level) {
        return plugin.getRuntimeConfig().getDouble("speed-levels." + level, 0.2);
    }

    public NamespacedKey getSpeedLevelKey() {
        return speedLevelKey;
    }

    public NamespacedKey getHarnessColorKey() {
        return harnessColorKey;
    }

    public void applySpeedEffect(LivingEntity entity, ItemStack harness) {
        if (harness == null) {
            return;
        }

        int level = getSpeedLevel(harness);
        if (level <= 0) {
            return;
        }

        double speedBoost = getSpeedValue(level);
        if (speedBoost <= 0) {
            return;
        }

        AttributeInstance attribute = entity.getAttribute(Attribute.GENERIC_FLYING_SPEED);

        if (attribute == null) {
            return;
        }

        NamespacedKey modifierKey = new NamespacedKey(plugin, "ghostspeed_boost");
        attribute.removeModifier(modifierKey);

        AttributeModifier modifier = new AttributeModifier(
                modifierKey,
                speedBoost,
                AttributeModifier.Operation.ADD_NUMBER
        );
        attribute.addModifier(modifier);

        UUID entityId = entity.getUniqueId();

        ScheduledTask existingTask = entityTasks.get(entityId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        long interval = Math.max(2L, plugin.getRuntimeConfig().getLong("collision-check-interval-ticks", 4L));
        ScheduledTask task = entity.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (!entity.isValid() || entity.getPassengers().isEmpty()) {
                removeSpeedEffect(entity);
                scheduledTask.cancel();
                entityTasks.remove(entityId);
                return;
            }

            ItemStack equippedHarness = getEquippedHarness(entity);
            if (equippedHarness == null || !isSpeedHarness(equippedHarness)) {
                removeSpeedEffect(entity);
                return;
            }

            Location currentLoc = entity.getLocation();
            Location lastLoc = lastLocations.get(entityId);

            if (lastLoc != null && lastLoc.getWorld().equals(currentLoc.getWorld())) {
                double dist = Math.sqrt(Math.pow(currentLoc.getX() - lastLoc.getX(), 2) + Math.pow(currentLoc.getZ() - lastLoc.getZ(), 2));

                if (dist > 0.15) {
                    org.bukkit.util.Vector horizontalDir = currentLoc.getDirection();
                    horizontalDir.setY(0);
                    if (horizontalDir.lengthSquared() > 0) {
                        horizontalDir.normalize();
                    }

                    Location checkLoc = currentLoc.clone().add(horizontalDir.multiply(2.8)).add(0, 1.5, 0);

                    if (checkLoc.getBlock().getType().isSolid()) {
                        long now = System.currentTimeMillis();
                        long lastHit = lastCollisionTimes.getOrDefault(entityId, 0L);

                        if (now - lastHit > 1000L) {
                            lastCollisionTimes.put(entityId, now);

                            double damage = Math.min(4.0, dist * 15.0);

                            if (entity.getHealth() <= damage) {
                                org.bukkit.entity.Player player = (org.bukkit.entity.Player) entity.getPassengers().get(0);
                                String tailNumber = plugin.getTailNumberManager().getTailNumber(entity);
                                int harnessLevel = getSpeedLevel(equippedHarness);
                                String planeType = plugin.getFlightSystemManager().getPlaneTypeName(harnessLevel);

                                if (entity.getEquipment() != null) {
                                    entity.getEquipment().setItem(org.bukkit.inventory.EquipmentSlot.BODY, null);
                                    entity.getEquipment().setItem(org.bukkit.inventory.EquipmentSlot.CHEST, null);
                                    entity.getEquipment().setDropChance(org.bukkit.inventory.EquipmentSlot.BODY, 0.0f);
                                    entity.getEquipment().setDropChance(org.bukkit.inventory.EquipmentSlot.CHEST, 0.0f);
                                    entity.getEquipment().clear();
                                }
                                entity.remove();

                                boolean damageBlocks = plugin.getRuntimeConfig().getBoolean("collision-explosion-damages-blocks", false);
                                currentLoc.getWorld().createExplosion(currentLoc, 4.0f, false, damageBlocks);

                                player.setHealth(0);
                                String safeTailNumber = (tailNumber != null) ? tailNumber : "未知";
                                player.sendMessage(org.bukkit.ChatColor.RED + "💥 您的" + planeType + " " + safeTailNumber + "飞机因撞击建筑而爆炸！");
                                plugin.getTailNumberManager().logCrash(player, entity, "撞击建筑");

                                removeSpeedEffect(entity);
                                scheduledTask.cancel();
                                return;
                            } else {
                                entity.damage(damage);
                                entity.getWorld().playSound(currentLoc, Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.0f, 1.0f);
                            }
                        }
                    }
                }
            }
            lastLocations.put(entityId, currentLoc);

            if (entity.getHealth() <= 5.0) {
                entity.getWorld().spawnParticle(org.bukkit.Particle.CAMPFIRE_COSY_SMOKE, currentLoc.clone().add(0, 2, 0), 5, 0.5, 0.5, 0.5, 0.05);
            }

        }, null, interval, interval);

        entityTasks.put(entityId, task);
    }

    public void removeSpeedEffect(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attribute.GENERIC_FLYING_SPEED);

        if (attribute == null) {
            return;
        }

        NamespacedKey modifierKey = new NamespacedKey(plugin, "ghostspeed_boost");
        attribute.removeModifier(modifierKey);

        UUID entityId = entity.getUniqueId();
        ScheduledTask task = entityTasks.remove(entityId);
        if (task != null) {
            task.cancel();
        }
        lastLocations.remove(entityId);
        lastCollisionTimes.remove(entityId);
    }

    private ItemStack getEquippedHarness(LivingEntity entity) {
        var equipment = entity.getEquipment();
        if (equipment == null) {
            return null;
        }

        ItemStack item = equipment.getItem(org.bukkit.inventory.EquipmentSlot.BODY);
        if (item != null && item.getType().name().endsWith("_HARNESS")) {
            return item;
        }

        item = equipment.getItem(org.bukkit.inventory.EquipmentSlot.CHEST);
        if (item != null && item.getType().name().endsWith("_HARNESS")) {
            return item;
        }

        return null;
    }
}
