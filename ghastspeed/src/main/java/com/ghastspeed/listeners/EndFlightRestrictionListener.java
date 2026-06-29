package com.ghastspeed.listeners;

import com.ghastspeed.GhostSpeed;
import com.ghastspeed.items.SpeedHarness;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Prevents assembling a plugin aircraft by applying a speed harness in The End. */
public final class EndFlightRestrictionListener implements Listener {

    private final GhostSpeed plugin;
    private final SpeedHarness speedHarness;

    public EndFlightRestrictionListener(GhostSpeed plugin) {
        this.plugin = plugin;
        this.speedHarness = plugin.getSpeedHarness();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHarnessApply(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof LivingEntity entity) || !isHappyGhast(entity)) {
            return;
        }
        if (!plugin.isFlightBlocked(entity.getWorld())) {
            return;
        }

        ItemStack heldItem = event.getHand() == EquipmentSlot.HAND
                ? event.getPlayer().getInventory().getItemInMainHand()
                : event.getPlayer().getInventory().getItemInOffHand();
        if (!speedHarness.isSpeedHarness(heldItem)) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(plugin.getEndFlightMessage());
    }

    private boolean isHappyGhast(LivingEntity entity) {
        String typeName = entity.getType().name();
        return typeName.equals("HAPPY_GHAST") || typeName.contains("GHAST");
    }
}
