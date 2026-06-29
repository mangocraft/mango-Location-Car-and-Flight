package com.mangolocation.command;

import com.mangolocation.MangoLocation;
import com.mangolocation.api.Area;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class MangoLocationCommand implements CommandExecutor, TabCompleter {

    private final MangoLocation plugin;

    public MangoLocationCommand(MangoLocation plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("where")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "该命令只能由玩家执行。");
                return true;
            }
            Area area = plugin.getAreaService().findArea(player).orElse(null);
            sender.sendMessage(area == null
                    ? ChatColor.GRAY + "你目前不在任何已配置区域内。"
                    : ChatColor.GREEN + "你目前位于 " + ChatColor.YELLOW + area.name()
                    + ChatColor.GRAY + " (" + area.id() + ")");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mangolocation.reload")) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行该命令。");
                return true;
            }
            plugin.reloadAreas(sender);
            sender.sendMessage(ChatColor.YELLOW + "正在异步重载 MangoLocation……");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "/" + label + " [where|reload]");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        return List.of("where", "reload").stream()
                .filter(value -> value.startsWith(args[0].toLowerCase()))
                .filter(value -> !value.equals("reload") || sender.hasPermission("mangolocation.reload"))
                .toList();
    }
}
