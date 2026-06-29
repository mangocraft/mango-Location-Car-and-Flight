package com.ghastspeed.commands;

import com.ghastspeed.GhostSpeed;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;

public class ReloadCommand implements CommandExecutor {
    private final GhostSpeed plugin;

    public ReloadCommand(GhostSpeed plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("ghastspeed.reload")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令!");
            return true;
        }

        plugin.getFlightSystemManager().reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "配置文件已重新加载!");
        return true;
    }
}
