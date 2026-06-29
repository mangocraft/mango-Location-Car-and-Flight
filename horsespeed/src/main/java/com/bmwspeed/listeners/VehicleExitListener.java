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
import org.bukkit.event.vehicle.VehicleExitEvent;

public class VehicleExitListener implements Listener {

    private final BmwSpeed plugin;
    private final DriveSystemManager driveManager;
    private final LicensePlateManager plateManager;
    private final SpeedEngine speedEngine;

    public VehicleExitListener(BmwSpeed plugin, DriveSystemManager driveManager, LicensePlateManager plateManager) {
        this.plugin = plugin;
        this.driveManager = driveManager;
        this.plateManager = plateManager;
        this.speedEngine = plugin.getSpeedEngine();
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player player)) {
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
            speedEngine.removeSpeedEffect(horse);
            driveManager.clearDriveData(horse);
            horse.setAI(false);
            player.sendMessage(ChatColor.GREEN + "✅ VIP 特权：随时随地安全停车！");
            return;
        }

        long fuelExpire = driveManager.getFuelExpire(horse);
        if (fuelExpire == 0) {
            speedEngine.removeSpeedEffect(horse);
            driveManager.clearDriveData(horse);
            horse.setAI(false);
            player.sendMessage(ChatColor.YELLOW + "您已下车，未支付油费。");
            return;
        }

        speedEngine.removeSpeedEffect(horse);
        horse.setAI(false);
        player.sendMessage(ChatColor.GREEN + "✅ 已安全停车！剩余燃油已保留。");
    }
}
