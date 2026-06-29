package com.bmwspeed.commands;

import com.bmwspeed.BmwSpeed;
import com.bmwspeed.items.SpeedEngine;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CarSpeedCommand implements CommandExecutor {

    private final BmwSpeed plugin;
    private final SpeedEngine speedEngine;

    public CarSpeedCommand(BmwSpeed plugin) {
        this.plugin = plugin;
        this.speedEngine = plugin.getSpeedEngine();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "你没有权限使用此指令！");
                return true;
            }
            plugin.reloadPluginConfig();
            sender.sendMessage(ChatColor.GREEN + "BmwSpeed 配置文件已重新加载！");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "用法：/carspeed give <玩家> <等级> <颜色>");
            sender.sendMessage(ChatColor.YELLOW + "用法：/carspeed reload");
            sender.sendMessage(ChatColor.YELLOW + "颜色：white, orange, magenta, light_blue, yellow, lime, pink, gray, light_gray, cyan, purple, blue, brown, green, red, black");
            return true;
        }

        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此指令！");
            return true;
        }

        if (!args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(ChatColor.RED + "未知子命令！用法：/carspeed give <玩家> <等级> <颜色> 或 /carspeed reload");
            return true;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "玩家不在线！");
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
            if (level < 1 || level > 6) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "等级必须是 1-6 之间的数字！");
            return true;
        }

        String colorName = args[3].toLowerCase();
        org.bukkit.Material woolMaterial;
        try {
            woolMaterial = org.bukkit.Material.valueOf(colorName.toUpperCase() + "_WOOL");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "无效的颜色！可用颜色：white, orange, magenta, light_blue, yellow, lime, pink, gray, light_gray, cyan, purple, blue, brown, green, red, black");
            return true;
        }

        ItemStack engine = speedEngine.createSpeedEngine(level, colorName, woolMaterial);
        String carType = getCarTypeName(level);
        String targetName = target.getName();
        target.getScheduler().run(plugin, task -> {
            target.getInventory().addItem(engine);
            target.sendMessage(ChatColor.GREEN + "你获得了 " + carType + " 引擎！使用马鞍装备到你的马匹上！");
            sendToSender(sender, ChatColor.GREEN + "已成功给予 " + targetName + " " + level + " 级 " + carType + " 引擎！");
        }, () -> sendToSender(sender, ChatColor.RED + "发放失败：目标玩家已经离线。"));

        return true;
    }

    private void sendToSender(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            player.getScheduler().run(plugin, task -> player.sendMessage(message), null);
        } else {
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> sender.sendMessage(message));
        }
    }

    private String getCarTypeName(int level) {
        switch (level) {
            case 6:
                return "宝马 M8";
            case 5:
                return "宝马 M5";
            case 4:
                return "宝马 7 系";
            case 3:
                return "宝马 5 系";
            case 2:
                return "宝马 3 系";
            case 1:
                return "宝马 1 系";
            default:
                return "宝马跑车";
        }
    }
}
