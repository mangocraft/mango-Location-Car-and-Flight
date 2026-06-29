package com.ghastspeed.listeners;

import com.ghastspeed.GhostSpeed;
import com.ghastspeed.flight.TailNumberManager;
import com.ghastspeed.items.SpeedHarness;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HarnessListener {

    private final GhostSpeed plugin;
    private final SpeedHarness speedHarness;
    private final TailNumberManager tailNumberManager;
    private final Map<UUID, ScheduledTask> entityTasks;
    private boolean debugMode;

    public HarnessListener(GhostSpeed plugin) {
        this.plugin = plugin;
        this.speedHarness = plugin.getSpeedHarness();
        this.tailNumberManager = plugin.getTailNumberManager();
        this.entityTasks = new ConcurrentHashMap<>();
        this.debugMode = plugin.getRuntimeConfig().getBoolean("debug", false);
    }

    public void applySpeedEffect(LivingEntity entity, ItemStack harness) {
        if (!isHappyGhast(entity)) {
            return;
        }

        int level = speedHarness.getSpeedLevel(harness);
        if (level <= 0) {
            return;
        }

        double speedBoost = speedHarness.getSpeedValue(level);
        if (speedBoost <= 0) {
            return;
        }

        AttributeInstance attribute = entity.getAttribute(Attribute.GENERIC_FLYING_SPEED);

        if (attribute == null) {
            if (debugMode) {
                plugin.getLogger().warning("DEBUG: 找不到飞行速度属性！");
            }
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

        if (debugMode) {
            plugin.getLogger().info("DEBUG: 速度修饰符成功附加！增加值: " + speedBoost);
        }

        UUID entityId = entity.getUniqueId();

        ScheduledTask existingTask = entityTasks.get(entityId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        ScheduledTask task = entity.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (!entity.isValid()) {
                scheduledTask.cancel();
                entityTasks.remove(entityId);
                return;
            }

            ItemStack equippedHarness = getEquippedHarness(entity);
            if (equippedHarness == null || !speedHarness.isSpeedHarness(equippedHarness)) {
                removeSpeedEffect(entity);
            }
        }, null, 20L, 20L);

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
    }

    public void checkAndApplyHarness(LivingEntity entity) {
        if (debugMode) {
            plugin.getLogger().info("DEBUG: checkAndApplyHarness 已禁用 - 所有等级现在通过VehicleEnterListener统一处理");
        }
    }

    private ItemStack getEquippedHarness(LivingEntity entity) {
        if (!isHappyGhast(entity)) {
            return null;
        }

        var equipment = entity.getEquipment();
        if (equipment == null) {
            if (debugMode) {
                plugin.getLogger().info("DEBUG: 实体对应槽位为空或不是护具");
            }
            return null;
        }

        ItemStack item = equipment.getItem(EquipmentSlot.BODY);
        if (item != null && isHarnessItem(item.getType())) {
            if (debugMode) {
                plugin.getLogger().info("DEBUG: 成功读取到护具材质: " + item.getType());
            }
            return item;
        }

        item = equipment.getItem(EquipmentSlot.CHEST);
        if (item != null && isHarnessItem(item.getType())) {
            if (debugMode) {
                plugin.getLogger().info("DEBUG: 成功读取到护具材质: " + item.getType());
            }
            return item;
        }

        if (debugMode) {
            plugin.getLogger().info("DEBUG: 实体对应槽位为空或不是护具");
        }
        return null;
    }

    private boolean isHappyGhast(LivingEntity entity) {
        String typeName = entity.getType().name();
        return typeName.equals("HAPPY_GHAST") || typeName.contains("GHAST");
    }

    private boolean isHarnessItem(Material material) {
        String name = material.name();
        return name.endsWith("_HARNESS");
    }
}
