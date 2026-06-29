package com.mangolocation;

import com.mangolocation.api.MangoLocationApi;
import com.mangolocation.command.MangoLocationCommand;
import com.mangolocation.internal.ConfiguredAreaService;
import com.mangolocation.listener.PlayerAreaTracker;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class MangoLocation extends JavaPlugin {

    private ConfiguredAreaService areaService;
    private PlayerAreaTracker areaTracker;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        areaService = new ConfiguredAreaService();
        try {
            int count = areaService.reload(getConfig());
            getLogger().info("已加载 " + count + " 个地理区域。");
        } catch (IllegalArgumentException exception) {
            getLogger().severe("区域配置无效：" + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getServicesManager().register(MangoLocationApi.class, areaService, this, ServicePriority.Normal);
        areaTracker = new PlayerAreaTracker(this, areaService);
        getServer().getPluginManager().registerEvents(areaTracker, this);

        PluginCommand command = getCommand("mangolocation");
        if (command != null) {
            MangoLocationCommand executor = new MangoLocationCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
    }

    public void reloadAreas(CommandSender sender) {
        getServer().getAsyncScheduler().runNow(this, task -> {
            try {
                YamlConfiguration loaded = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
                int count = areaService.reload(loaded);
                refreshOnlinePlayers();
                sendCommandMessage(sender, "§aMangoLocation 已重载，共 " + count + " 个区域。");
            } catch (IllegalArgumentException exception) {
                sendCommandMessage(sender, "§c重载失败：" + exception.getMessage());
            }
        });
    }

    private void refreshOnlinePlayers() {
        getServer().getGlobalRegionScheduler().execute(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                player.getScheduler().run(this, task -> areaTracker.refresh(player), null);
            }
        });
    }

    private void sendCommandMessage(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            player.getScheduler().run(this, task -> player.sendMessage(message), null);
        } else {
            getServer().getGlobalRegionScheduler().execute(this, () -> sender.sendMessage(message));
        }
    }

    public ConfiguredAreaService getAreaService() {
        return areaService;
    }
}
