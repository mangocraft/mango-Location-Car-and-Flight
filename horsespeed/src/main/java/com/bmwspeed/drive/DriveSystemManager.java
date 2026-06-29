package com.bmwspeed.drive;

import com.bmwspeed.BmwSpeed;
import com.bmwspeed.items.SpeedEngine;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DriveSystemManager {

    private final BmwSpeed plugin;
    private final SpeedEngine speedEngine;
    private final LicensePlateManager licensePlateManager;
    private Economy economy;
    private final Map<UUID, ScheduledTask> horseTasks;

    private final NamespacedKey FUEL_EXPIRE_KEY;
    private final NamespacedKey IS_CRASHED_KEY;
    private final NamespacedKey VEHICLE_LEVEL_KEY;
    private final NamespacedKey IS_IN_BUFFER_KEY;

    public DriveSystemManager(BmwSpeed plugin, LicensePlateManager licensePlateManager) {
        this.plugin = plugin;
        this.speedEngine = plugin.getSpeedEngine();
        this.licensePlateManager = licensePlateManager;
        this.horseTasks = new ConcurrentHashMap<>();

        this.FUEL_EXPIRE_KEY = new NamespacedKey(plugin, "fuel_expire");
        this.IS_CRASHED_KEY = new NamespacedKey(plugin, "is_crashed");
        this.VEHICLE_LEVEL_KEY = new NamespacedKey(plugin, "vehicle_level");
        this.IS_IN_BUFFER_KEY = new NamespacedKey(plugin, "is_in_buffer");

        setupEconomy();
        startDriveChecker();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean hasEconomy() {
        return economy != null;
    }

    public boolean canAfford(Player player, double amount) {
        if (!hasEconomy()) {
            return true;
        }
        return economy.has(player, amount);
    }

    public boolean withdrawMoney(Player player, double amount) {
        if (!hasEconomy()) {
            return true;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public int getVehicleLevel(AbstractHorse horse) {
        ItemStack engine = getEquippedEngine(horse);
        if (engine != null && speedEngine.isSpeedEngine(engine)) {
            int level = speedEngine.getEngineLevel(engine);
            if (level >= 1 && level <= 6) {
                setVehicleLevel(horse, level);
                return level;
            }
        }

        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        if (pdc.has(VEHICLE_LEVEL_KEY, PersistentDataType.INTEGER)) {
            return pdc.get(VEHICLE_LEVEL_KEY, PersistentDataType.INTEGER);
        }
        return 0;
    }

    public void setVehicleLevel(AbstractHorse horse, int level) {
        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        pdc.set(VEHICLE_LEVEL_KEY, PersistentDataType.INTEGER, level);
    }

    public long getFuelExpire(AbstractHorse horse) {
        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        if (pdc.has(FUEL_EXPIRE_KEY, PersistentDataType.LONG)) {
            return pdc.get(FUEL_EXPIRE_KEY, PersistentDataType.LONG);
        }
        return 0;
    }

    public void setFuelExpire(AbstractHorse horse, long timestamp) {
        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        pdc.set(FUEL_EXPIRE_KEY, PersistentDataType.LONG, timestamp);
    }

    public boolean isInBuffer(AbstractHorse horse) {
        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        return pdc.has(IS_IN_BUFFER_KEY, PersistentDataType.BYTE) &&
               pdc.get(IS_IN_BUFFER_KEY, PersistentDataType.BYTE) == 1;
    }

    public void clearDriveData(AbstractHorse horse) {
        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        pdc.remove(FUEL_EXPIRE_KEY);
        pdc.remove(IS_CRASHED_KEY);
        pdc.remove(IS_IN_BUFFER_KEY);
    }

    public double getPrice(int level) {
        switch (level) {
            case 1:
                return 40.0;
            case 2:
                return 60.0;
            case 3:
                return 80.0;
            case 4:
                return 100.0;
            case 5:
                return plugin.getRuntimeConfig().getDouble("level5-car.price", 500.0);
            case 6:
                return plugin.getRuntimeConfig().getDouble("level6-car.price", 1000.0);
            default:
                return 0;
        }
    }

    public int getDriveDuration(int level) {
        if (level == 6) {
            return plugin.getRuntimeConfig().getInt("level6-car.fuel-duration", 30);
        }
        return 15;
    }

    public int getBufferDuration(int level) {
        return 5;
    }

    public String getPermission(int level) {
        switch (level) {
            case 1:
                return "bmwspeed.vipdrive.1";
            case 2:
                return "bmwspeed.vipdrive.2";
            case 3:
                return "bmwspeed.vipdrive.3";
            case 4:
                return "bmwspeed.vipdrive.4";
            case 5:
                return plugin.getRuntimeConfig().getString("level5-car.permission", "bmwspeed.vipdrive.5");
            case 6:
                return plugin.getRuntimeConfig().getString("level6-car.permission", "bmwspeed.vipdrive.6");
            default:
                return "";
        }
    }

    private void startDriveChecker() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getScheduler().run(plugin, scheduledTask -> {
                    if (!player.isValid()) {
                        return;
                    }
                    if (player.getVehicle() instanceof AbstractHorse horse) {
                        if (!horse.isValid()) {
                            return;
                        }
                        int level = getVehicleLevel(horse);
                        if (level >= 1 && level <= 6) {
                            checkDriveStatus(player, horse, level);
                        }
                    }
                }, null);
            }
        }, 20L, 20L);
    }

    private void checkDriveStatus(Player player, AbstractHorse horse, int level) {
        if (player.hasPermission(getPermission(level))) {
            return;
        }

        long fuelExpire = getFuelExpire(horse);
        if (fuelExpire == 0) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < fuelExpire) {
            return;
        }

        int bufferMinutes = getBufferDuration(level);
        long bufferEnd = fuelExpire + (bufferMinutes * 60 * 1000L);

        PersistentDataContainer pdc = horse.getPersistentDataContainer();

        if (now < bufferEnd) {
            // 正确的写法：提示必须写在大括号【里面】！
            if (!pdc.has(IS_IN_BUFFER_KEY, PersistentDataType.BYTE)) {
                pdc.set(IS_IN_BUFFER_KEY, PersistentDataType.BYTE, (byte) 1);
                speedEngine.removeSpeedEffect(horse);
                player.sendTitle(ChatColor.RED + "⚠️ 油箱已见底！", ChatColor.YELLOW + "请在 5 分钟内输入 /addgas 加油，否则车辆将抛锚！", 10, 70, 20);
            }
        } else {
            // 加入 IS_CRASHED_KEY 防刷屏锁
            if (!pdc.has(IS_CRASHED_KEY, PersistentDataType.BYTE)) {
                pdc.set(IS_CRASHED_KEY, PersistentDataType.BYTE, (byte) 1);
                horse.setAI(false); // 失去动力
                speedEngine.removeSpeedEffect(horse);
                pdc.remove(IS_IN_BUFFER_KEY);

                player.sendMessage(ChatColor.RED + "⛽ 燃油已彻底耗尽，车辆已抛锚失去动力！");
                player.sendMessage(ChatColor.YELLOW + "💡 请输入 " + ChatColor.GREEN + "/addgas" + ChatColor.YELLOW + " 重新加满油箱启动引擎！");
            }
        }
    }

    private ItemStack getEquippedEngine(AbstractHorse horse) {
        if (horse.getInventory() == null) {
            return null;
        }
        ItemStack item = horse.getInventory().getSaddle();
        if (item != null && item.getType() == Material.SADDLE) {
            return item;
        }
        return null;
    }

    public void reloadConfig() {
        plugin.reloadPluginConfig();
    }

    public String getCarTypeName(int level) {
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
                return "未知车型";
        }
    }
}
