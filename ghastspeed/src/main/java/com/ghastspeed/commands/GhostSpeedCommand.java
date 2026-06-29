package com.ghastspeed.commands;

import com.ghastspeed.GhostSpeed;
import com.ghastspeed.items.SpeedHarness;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GhostSpeedCommand implements CommandExecutor {

    private final GhostSpeed plugin;
    private final SpeedHarness speedHarness;

    public GhostSpeedCommand(GhostSpeed plugin) {
        this.plugin = plugin;
        this.speedHarness = plugin.getSpeedHarness();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "你没有权限使用此指令！");
                return true;
            }
            plugin.reloadPluginConfig();
            sender.sendMessage(ChatColor.GREEN + "GhostSpeed 配置文件已重新加载！");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "用法: /ghastspeed give <玩家> <等级> <颜色>");
            sender.sendMessage(ChatColor.YELLOW + "用法: /ghastspeed reload");
            sender.sendMessage(ChatColor.YELLOW + "颜色: white, orange, magenta, light_blue, yellow, lime, pink, gray, light_gray, cyan, purple, blue, brown, green, red, black");
            return true;
        }

        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此指令！");
            return true;
        }

        if (!args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(ChatColor.RED + "未知子命令！用法: /ghastspeed give <玩家> <等级> <颜色> 或 /ghastspeed reload");
            return true;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "玩家 " + playerName + " 不在线！");
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "等级必须是数字！");
            return true;
        }

        if (level < 1 || level > 6) {
            sender.sendMessage(ChatColor.RED + "等级必须在1-6之间！");
            return true;
        }

        String colorName = args[3].toLowerCase();
        Material woolMaterial = getWoolMaterial(colorName);

        if (woolMaterial == null) {
            sender.sendMessage(ChatColor.RED + "无效的颜色！可用颜色: white, orange, magenta, light_blue, yellow, lime, pink, gray, light_gray, cyan, purple, blue, brown, green, red, black");
            return true;
        }

        String colorDisplayName = getColorDisplayName(colorName);
        ItemStack harness = speedHarness.createSpeedHarness(level, colorDisplayName, woolMaterial);
        String targetName = target.getName();
        target.getScheduler().run(plugin, task -> {
            target.getInventory().addItem(harness);
            target.sendMessage(ChatColor.GREEN + "你获得了 " + colorDisplayName + " 速度护具 " + level + " 级！");
            sendToSender(sender, ChatColor.GREEN + "已成功给予 " + targetName + " 速度护具！");
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

    private Material getWoolMaterial(String color) {
        return switch (color) {
            case "white" -> Material.WHITE_WOOL;
            case "orange" -> Material.ORANGE_WOOL;
            case "magenta" -> Material.MAGENTA_WOOL;
            case "light_blue" -> Material.LIGHT_BLUE_WOOL;
            case "yellow" -> Material.YELLOW_WOOL;
            case "lime" -> Material.LIME_WOOL;
            case "pink" -> Material.PINK_WOOL;
            case "gray" -> Material.GRAY_WOOL;
            case "light_gray" -> Material.LIGHT_GRAY_WOOL;
            case "cyan" -> Material.CYAN_WOOL;
            case "purple" -> Material.PURPLE_WOOL;
            case "blue" -> Material.BLUE_WOOL;
            case "brown" -> Material.BROWN_WOOL;
            case "green" -> Material.GREEN_WOOL;
            case "red" -> Material.RED_WOOL;
            case "black" -> Material.BLACK_WOOL;
            default -> null;
        };
    }

    private String getColorDisplayName(String color) {
        return switch (color) {
            case "white" -> "白色";
            case "orange" -> "橙色";
            case "magenta" -> "品红色";
            case "light_blue" -> "淡蓝色";
            case "yellow" -> "黄色";
            case "lime" -> "黄绿色";
            case "pink" -> "粉红色";
            case "gray" -> "灰色";
            case "light_gray" -> "淡灰色";
            case "cyan" -> "青色";
            case "purple" -> "紫色";
            case "blue" -> "蓝色";
            case "brown" -> "棕色";
            case "green" -> "绿色";
            case "red" -> "红色";
            case "black" -> "黑色";
            default -> "白色";
        };
    }
}
