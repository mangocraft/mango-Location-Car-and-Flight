package com.ghastspeed.listeners;

import com.ghastspeed.GhostSpeed;
import com.ghastspeed.flight.TailNumberManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class EntityMountListener implements Listener {

    private final GhostSpeed plugin;
    private final HarnessListener harnessListener;
    private final TailNumberManager tailNumberManager;

    public EntityMountListener(GhostSpeed plugin, HarnessListener harnessListener, TailNumberManager tailNumberManager) {
        this.plugin = plugin;
        this.harnessListener = harnessListener;
        this.tailNumberManager = tailNumberManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player)) {
            return;
        }

        if (!(event.getVehicle() instanceof LivingEntity entity)) {
            return;
        }

        if (!isHappyGhast(entity)) {
            return;
        }

        plugin.getServer().getRegionScheduler().run(plugin, entity.getLocation(), task -> {
            harnessListener.checkAndApplyHarness(entity);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        if (!isHappyGhast(entity)) {
            return;
        }

        plugin.getServer().getRegionScheduler().run(plugin, entity.getLocation(), task -> {
            harnessListener.checkAndApplyHarness(entity);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity clickedEntity = event.getRightClicked();

        if (!(clickedEntity instanceof LivingEntity entity)) {
            return;
        }

        if (!isHappyGhast(entity)) {
            return;
        }

        entity.getScheduler().runDelayed(plugin, task -> {
            harnessListener.checkAndApplyHarness(entity);
        }, null, 1L);
    }

    private boolean isHappyGhast(LivingEntity entity) {
        String typeName = entity.getType().name();
        return typeName.equals("HAPPY_GHAST") || typeName.contains("GHAST");
    }
}
