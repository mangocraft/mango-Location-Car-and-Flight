package com.bmwspeed.commands;

import com.bmwspeed.BmwSpeed;
import com.bmwspeed.drive.DriveSystemManager;
import com.bmwspeed.drive.LicensePlateManager;
import com.bmwspeed.items.SpeedEngine;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;

public class AddGasCommand implements CommandExecutor {

    private final BmwSpeed plugin;
    private final DriveSystemManager driveManager;
    private final LicensePlateManager plateManager;
    private final SpeedEngine speedEngine;

    public AddGasCommand(BmwSpeed plugin, DriveSystemManager driveManager, LicensePlateManager plateManager) {
        this.plugin = plugin;
        this.driveManager = driveManager;
        this.plateManager = plateManager;
        this.speedEngine = plugin.getSpeedEngine();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此指令！");
            return true;
        }

        if (!(player.getVehicle() instanceof AbstractHorse horse)) {
            player.sendMessage(ChatColor.RED + "你必须正在驾驶宝马跑车！");
            return true;
        }

        int level = driveManager.getVehicleLevel(horse);
        if (level < 1 || level > 6) {
            player.sendMessage(ChatColor.RED + "你必须正在驾驶 1-6 级宝马跑车！");
            return true;
        }

        long fuelExpire = driveManager.getFuelExpire(horse);
        long now = System.currentTimeMillis();

        if (fuelExpire > now) {
            player.sendMessage(ChatColor.YELLOW + "燃油充足，无需加油！");
            return true;
        }

        double price = driveManager.getPrice(level);
        int duration = driveManager.getDriveDuration(level);

        if (!driveManager.hasEconomy()) {
            player.sendMessage(ChatColor.RED + "经济系统未启用，无法扣费！");
            return true;
        }

        if (!driveManager.canAfford(player, price)) {
            player.sendMessage(ChatColor.RED + "你没有足够的钱！需要 " + price + " 元油钱！");
            return true;
        }

        if (!driveManager.withdrawMoney(player, price)) {
            player.sendMessage(ChatColor.RED + "扣费失败！");
            return true;
        }

        long newExpire = now + (duration * 60 * 1000L);
        driveManager.setFuelExpire(horse, newExpire);

        horse.getScheduler().run(plugin, scheduledTask -> {
            if (!horse.isValid() || !player.isValid()) {
                return;
            }

            horse.setAI(true);
            speedEngine.applySpeedEffect(horse, getEquippedEngine(horse));
            plateManager.logDriveStart(player, horse);

            player.sendMessage(ChatColor.GREEN + "🚗 支付成功！已缴纳 " + price + " 元油钱，启动引擎！获得 " + duration + " 分钟驾驶时间！");
        }, null);

        return true;
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
