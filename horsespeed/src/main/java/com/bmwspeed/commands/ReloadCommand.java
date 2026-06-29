package com.bmwspeed.commands;

import com.bmwspeed.BmwSpeed;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final BmwSpeed plugin;

    public ReloadCommand(BmwSpeed plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此指令！");
            return true;
        }

        plugin.reloadPluginConfig();
        sender.sendMessage(ChatColor.GREEN + "BmwSpeed 配置文件已重新加载！");
        return true;
    }
}
