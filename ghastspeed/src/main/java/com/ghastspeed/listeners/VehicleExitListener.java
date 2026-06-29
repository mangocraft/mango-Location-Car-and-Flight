package com.ghastspeed.listeners;

import com.ghastspeed.GhostSpeed;
import com.ghastspeed.flight.FlightSystemManager;
import com.ghastspeed.flight.TailNumberManager;
import com.ghastspeed.items.SpeedHarness;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class VehicleExitListener implements Listener {

    private final GhostSpeed plugin;
    private final FlightSystemManager flightManager;
    private final TailNumberManager tailNumberManager;
    private final SpeedHarness speedHarness;

    public VehicleExitListener(GhostSpeed plugin, FlightSystemManager flightManager, TailNumberManager tailNumberManager) {
        this.plugin = plugin;
        this.flightManager = flightManager;
        this.tailNumberManager = tailNumberManager;
        this.speedHarness = plugin.getSpeedHarness();
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player player)) {
            return;
        }

        if (!(event.getVehicle() instanceof LivingEntity vehicle)) {
            return;
        }

        int level = flightManager.getVehicleLevel(vehicle);
        if (level < 1 || level > 6) {
            return;
        }

        String permission = flightManager.getPermission(level);
        if (player.hasPermission(permission)) {
            speedHarness.removeSpeedEffect(vehicle);
            flightManager.clearFlightData(vehicle);
            vehicle.setAI(false);
            player.sendMessage(ChatColor.GREEN + "✅ VIP特权：随时随地安全降落！");
            return;
        }

        long fuelExpire = flightManager.getFuelExpire(vehicle);
        if (fuelExpire == 0) {
            speedHarness.removeSpeedEffect(vehicle);
            flightManager.clearFlightData(vehicle);
            vehicle.setAI(false);
            player.sendMessage(ChatColor.YELLOW + "您已下车，未支付油费。");
            return;
        }

        if (flightManager.isCrashed(vehicle)) {
            if (vehicle.getEquipment() != null) {
                vehicle.getEquipment().setItem(org.bukkit.inventory.EquipmentSlot.BODY, null);
                vehicle.getEquipment().setItem(org.bukkit.inventory.EquipmentSlot.CHEST, null);
                vehicle.getEquipment().setDropChance(org.bukkit.inventory.EquipmentSlot.BODY, 0.0f);
                vehicle.getEquipment().setDropChance(org.bukkit.inventory.EquipmentSlot.CHEST, 0.0f);
                vehicle.getEquipment().clear();
            }
            Location loc = vehicle.getLocation();
            vehicle.remove();
            loc.getWorld().createExplosion(loc, 4.0f, false, false);
            player.setHealth(0);
            tailNumberManager.logCrash(player, vehicle, "燃油耗尽");
            player.sendMessage(ChatColor.RED + "💥 轰！油量耗尽坠机！");
            speedHarness.removeSpeedEffect(vehicle);
            flightManager.clearFlightData(vehicle);
            return;
        }

        if (!flightManager.isInAllowedRegion(player.getLocation(), level)) {
            if (level >= 5) {
                if (vehicle.getEquipment() != null) {
                    vehicle.getEquipment().setItem(org.bukkit.inventory.EquipmentSlot.BODY, null);
                    vehicle.getEquipment().setItem(org.bukkit.inventory.EquipmentSlot.CHEST, null);
                    vehicle.getEquipment().setDropChance(org.bukkit.inventory.EquipmentSlot.BODY, 0.0f);
                    vehicle.getEquipment().setDropChance(org.bukkit.inventory.EquipmentSlot.CHEST, 0.0f);
                    vehicle.getEquipment().clear();
                }
                Location loc = vehicle.getLocation();
                vehicle.remove();
                loc.getWorld().createExplosion(loc, 4.0f, false, false);
                player.setHealth(0);
                tailNumberManager.logCrash(player, vehicle, "违规降落");
                player.sendMessage(ChatColor.RED + "💥 轰！未按规定在停机坪降落，降落伞失灵坠机！");
                speedHarness.removeSpeedEffect(vehicle);
                flightManager.clearFlightData(vehicle);
                return;
            } else {
                speedHarness.removeSpeedEffect(vehicle);
                vehicle.setAI(false);
                player.sendMessage(ChatColor.YELLOW + "⚠️ 警告：您未在规定的停机坪/机场下机！直升机将持续悬浮并消耗燃油，耗尽后将坠毁！");
                flightManager.startHoverTask(vehicle, level);
                return;
            }
        }

        speedHarness.removeSpeedEffect(vehicle);
        vehicle.setAI(false);
        player.sendMessage(ChatColor.GREEN + "✅ 安全降落！剩余燃油已保留。");
    }
}
