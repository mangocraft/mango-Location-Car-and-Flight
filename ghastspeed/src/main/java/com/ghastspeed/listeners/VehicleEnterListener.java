package com.ghastspeed.listeners;

import com.ghastspeed.GhostSpeed;
import com.ghastspeed.flight.FlightSystemManager;
import com.ghastspeed.flight.TailNumberManager;
import com.ghastspeed.items.SpeedHarness;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class VehicleEnterListener implements Listener {

    private final GhostSpeed plugin;
    private final FlightSystemManager flightManager;
    private final TailNumberManager tailNumberManager;
    private final SpeedHarness speedHarness;

    public VehicleEnterListener(GhostSpeed plugin, FlightSystemManager flightManager, TailNumberManager tailNumberManager) {
        this.plugin = plugin;
        this.flightManager = flightManager;
        this.tailNumberManager = tailNumberManager;
        this.speedHarness = plugin.getSpeedHarness();
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) {
            return;
        }

        if (!(event.getVehicle() instanceof LivingEntity vehicle)) {
            return;
        }

        int level = flightManager.getVehicleLevel(vehicle);
        if (level < 1 || level > 6) {
            return;
        }

        if (plugin.isFlightBlocked(vehicle.getWorld())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getEndFlightMessage());
            return;
        }

        String permission = flightManager.getPermission(level);
        if (player.hasPermission(permission)) {
            tailNumberManager.assignTailNumber(vehicle, level);
            vehicle.setAI(true);
            speedHarness.applySpeedEffect(vehicle, getEquippedHarness(vehicle));
            tailNumberManager.logFlightTakeoff(player, vehicle);
            player.sendMessage(ChatColor.GREEN + "✈️ VIP权限激活，免费无限制飞行！");
            return;
        }

        long fuelExpire = flightManager.getFuelExpire(vehicle);
        long now = System.currentTimeMillis();
        long bufferEnd = fuelExpire + (flightManager.getBufferDuration(level) * 60 * 1000L);

        if (fuelExpire > 0 && now >= bufferEnd) {
            flightManager.clearFlightData(vehicle);
            fuelExpire = 0;
        }

        if (fuelExpire == 0 && !flightManager.isInAllowedRegion(vehicle.getLocation(), level)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "必须在指定的停机坪/机场领地起飞！");
            return;
        }

        if (fuelExpire > now) {
            tailNumberManager.assignTailNumber(vehicle, level);
            vehicle.setAI(true);
            speedHarness.applySpeedEffect(vehicle, getEquippedHarness(vehicle));
            tailNumberManager.logFlightTakeoff(player, vehicle);
            long remainingMinutes = (fuelExpire - now) / 60000;
            player.sendMessage(ChatColor.GREEN + "✈️ 燃油充足，继续飞行！剩余时间约 " + remainingMinutes + " 分钟。");
            return;
        }

        if (fuelExpire > 0 && now >= fuelExpire && now < bufferEnd) {
            tailNumberManager.assignTailNumber(vehicle, level);
            vehicle.setAI(true);
            flightManager.setInBuffer(vehicle, true);
            player.sendMessage(ChatColor.RED + "⚠️ 燃油已耗尽，处于低速缓冲期！请尽快输入 /ghastpay 续费！");
            return;
        }

        if (fuelExpire == 0) {
            tailNumberManager.assignTailNumber(vehicle, level);
            String tailNumber = tailNumberManager.getTailNumber(vehicle);
            String planeType = flightManager.getPlaneTypeName(level);
            double price = flightManager.getPrice(level);
            int duration = flightManager.getFlightDuration(level);
            boolean hasLandingRequirement = level >= 5;

            player.sendMessage(ChatColor.GOLD + "=============================");
            player.sendMessage(ChatColor.YELLOW + "您现在正在乘坐 " + ChatColor.AQUA + planeType + " " + ChatColor.WHITE + tailNumber);
            player.sendMessage(ChatColor.YELLOW + "需每 " + ChatColor.GREEN + duration + ChatColor.YELLOW + " 分钟花费 " + ChatColor.GOLD + price + ChatColor.YELLOW + " 元油钱");
            if (hasLandingRequirement) {
                player.sendMessage(ChatColor.RED + "⚠️ 注意：此机型必须在指定机场降落，否则会爆炸！");
            }
            player.sendMessage(ChatColor.YELLOW + "如确认加油并启动发动机，请输入 " + ChatColor.GREEN + "/ghastpay" + ChatColor.YELLOW + " 支付油费");
            player.sendMessage(ChatColor.GOLD + "=============================");
        }
    }

    private org.bukkit.inventory.ItemStack getEquippedHarness(LivingEntity entity) {
        var equipment = entity.getEquipment();
        if (equipment == null) {
            return null;
        }

        org.bukkit.inventory.ItemStack item = equipment.getItem(org.bukkit.inventory.EquipmentSlot.BODY);
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
