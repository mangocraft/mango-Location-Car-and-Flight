package com.bmwspeed.listeners;

import com.bmwspeed.BmwSpeed;
import com.bmwspeed.drive.DriveSystemManager;
import com.bmwspeed.drive.LicensePlateManager;
import com.bmwspeed.items.SpeedEngine;
import org.bukkit.ChatColor;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class VehicleEnterListener implements Listener {

    private final BmwSpeed plugin;
    private final DriveSystemManager driveManager;
    private final LicensePlateManager plateManager;
    private final SpeedEngine speedEngine;

    public VehicleEnterListener(BmwSpeed plugin, DriveSystemManager driveManager, LicensePlateManager plateManager) {
        this.plugin = plugin;
        this.driveManager = driveManager;
        this.plateManager = plateManager;
        this.speedEngine = plugin.getSpeedEngine();
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) {
            return;
        }

        if (!(event.getVehicle() instanceof AbstractHorse horse)) {
            return;
        }

        int level = driveManager.getVehicleLevel(horse);
        if (level < 1 || level > 6) {
            return;
        }

        String permission = driveManager.getPermission(level);
        if (player.hasPermission(permission)) {
            plateManager.assignPlate(horse, level);
            horse.setAI(true);
            speedEngine.applySpeedEffect(horse, getEquippedEngine(horse));
            plateManager.logDriveStart(player, horse);
            player.sendMessage(ChatColor.GREEN + "🚗 VIP 权限激活，免费无限制驾驶！");
            return;
        }

        long fuelExpire = driveManager.getFuelExpire(horse);
        long now = System.currentTimeMillis();
        long bufferEnd = fuelExpire + (driveManager.getBufferDuration(level) * 60 * 1000L);

        if (fuelExpire > 0 && now >= bufferEnd) {
            driveManager.clearDriveData(horse);
            fuelExpire = 0;
        }

        if (fuelExpire > now) {
            plateManager.assignPlate(horse, level);
            horse.setAI(true);
            speedEngine.applySpeedEffect(horse, getEquippedEngine(horse));
            plateManager.logDriveStart(player, horse);
            long remainingMinutes = (fuelExpire - now) / 60000;
            player.sendMessage(ChatColor.GREEN + "🚗 燃油充足，继续驾驶！剩余时间约 " + remainingMinutes + " 分钟。");
            return;
        }

        if (fuelExpire > 0 && now >= fuelExpire && now < bufferEnd) {
            plateManager.assignPlate(horse, level);
            horse.setAI(true);
            player.sendMessage(ChatColor.RED + "⚠️ 燃油已耗尽，处于缓冲期！请尽快输入 /addgas 加油！");
            return;
        }

        if (fuelExpire == 0) {
            plateManager.assignPlate(horse, level);
            String plate = plateManager.getPlate(horse);
            String carType = driveManager.getCarTypeName(level);
            double price = driveManager.getPrice(level);
            int duration = driveManager.getDriveDuration(level);

            player.sendMessage(ChatColor.GOLD + "=============================");
            player.sendMessage(ChatColor.YELLOW + "您现在正在驾驶 " + ChatColor.AQUA + carType + " " + ChatColor.WHITE + plate);
            player.sendMessage(ChatColor.YELLOW + "需每 " + ChatColor.GREEN + duration + ChatColor.YELLOW + " 分钟花费 " + ChatColor.GOLD + price + ChatColor.YELLOW + " 元油钱");
            player.sendMessage(ChatColor.YELLOW + "如确认加油并启动引擎，请输入 " + ChatColor.GREEN + "/addgas" + ChatColor.YELLOW + " 支付油费");
            player.sendMessage(ChatColor.GOLD + "=============================");
        }
    }

    private org.bukkit.inventory.ItemStack getEquippedEngine(AbstractHorse horse) {
        if (horse.getInventory() == null) {
            return null;
        }
        org.bukkit.inventory.ItemStack item = horse.getInventory().getSaddle();
        if (item != null && item.getType() == org.bukkit.Material.SADDLE) {
            return item;
        }
        return null;
    }
}
