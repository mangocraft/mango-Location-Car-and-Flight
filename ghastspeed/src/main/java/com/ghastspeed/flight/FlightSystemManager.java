package com.ghastspeed.flight;

import com.ghastspeed.GhostSpeed;
import com.ghastspeed.hooks.RegionHook;
import com.ghastspeed.hooks.ResidenceHook;
import com.ghastspeed.items.SpeedHarness;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FlightSystemManager {

    private final GhostSpeed plugin;
    private final SpeedHarness speedHarness;
    private final TailNumberManager tailNumberManager;
    private RegionHook regionHook;
    private Economy economy;
    private final Map<UUID, ScheduledTask> entityTasks;

    private final NamespacedKey FUEL_EXPIRE_KEY;
    private final NamespacedKey IS_IN_BUFFER_KEY;
    private final NamespacedKey IS_CRASHED_KEY;
    private final NamespacedKey VEHICLE_LEVEL_KEY;

    public FlightSystemManager(GhostSpeed plugin, TailNumberManager tailNumberManager) {
        this.plugin = plugin;
        this.speedHarness = plugin.getSpeedHarness();
        this.tailNumberManager = tailNumberManager;
        this.entityTasks = new ConcurrentHashMap<>();

        if (Bukkit.getPluginManager().getPlugin("Residence") != null) {
            try {
                this.regionHook = new ResidenceHook(plugin);
                plugin.getLogger().info("成功加载Residence领地支持");
            } catch (Throwable t) {
                plugin.getLogger().warning("加载Residence钩子失败: " + t.getMessage());
                this.regionHook = null;
            }
        } else {
            this.regionHook = null;
            plugin.getLogger().info("未检测到Residence插件，领地限制功能已禁用");
        }

        this.FUEL_EXPIRE_KEY = new NamespacedKey(plugin, "fuel_expire");
        this.IS_IN_BUFFER_KEY = new NamespacedKey(plugin, "is_in_buffer");
        this.IS_CRASHED_KEY = new NamespacedKey(plugin, "is_crashed");
        this.VEHICLE_LEVEL_KEY = new NamespacedKey(plugin, "vehicle_level");

        setupEconomy();
        startFlightChecker();
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

    public boolean isInAllowedRegion(Location location, int level) {
        if (regionHook != null) {
            try {
                return regionHook.isInAllowedRegion(location, level);
            } catch (Throwable t) {
                plugin.getLogger().warning("无法检查Residence区域: " + t.getMessage());
                return false;
            }
        }
        return true;
    }

    public int getVehicleLevel(LivingEntity entity) {
        ItemStack harness = getEquippedHarness(entity);
        if (harness != null && speedHarness.isSpeedHarness(harness)) {
            int level = speedHarness.getSpeedLevel(harness);
            if (level >= 1 && level <= 6) {
                setVehicleLevel(entity, level);
                return level;
            }
        }

        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (pdc.has(VEHICLE_LEVEL_KEY, PersistentDataType.INTEGER)) {
            return pdc.get(VEHICLE_LEVEL_KEY, PersistentDataType.INTEGER);
        }
        return 0;
    }

    public void setVehicleLevel(LivingEntity entity, int level) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(VEHICLE_LEVEL_KEY, PersistentDataType.INTEGER, level);
    }

    public long getFuelExpire(LivingEntity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (pdc.has(FUEL_EXPIRE_KEY, PersistentDataType.LONG)) {
            return pdc.get(FUEL_EXPIRE_KEY, PersistentDataType.LONG);
        }
        return 0;
    }

    public void setFuelExpire(LivingEntity entity, long timestamp) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(FUEL_EXPIRE_KEY, PersistentDataType.LONG, timestamp);
    }

    public boolean isInBuffer(LivingEntity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return pdc.has(IS_IN_BUFFER_KEY, PersistentDataType.BYTE) &&
               pdc.get(IS_IN_BUFFER_KEY, PersistentDataType.BYTE) == 1;
    }

    public void setInBuffer(LivingEntity entity, boolean inBuffer) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(IS_IN_BUFFER_KEY, PersistentDataType.BYTE, (byte) (inBuffer ? 1 : 0));
    }

    public boolean isCrashed(LivingEntity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return pdc.has(IS_CRASHED_KEY, PersistentDataType.BYTE) &&
               pdc.get(IS_CRASHED_KEY, PersistentDataType.BYTE) == 1;
    }

    public void setCrashed(LivingEntity entity, boolean crashed) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(IS_CRASHED_KEY, PersistentDataType.BYTE, (byte) (crashed ? 1 : 0));
    }

    public void clearFlightData(LivingEntity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.remove(FUEL_EXPIRE_KEY);
        pdc.remove(IS_IN_BUFFER_KEY);
        pdc.remove(IS_CRASHED_KEY);
    }

    public double getPrice(int level) {
        switch (level) {
            case 1:
                return 400.0;
            case 2:
                return 600.0;
            case 3:
                return 800.0;
            case 4:
                return 1000.0;
            case 5:
                return plugin.getRuntimeConfig().getDouble("level5-ghast.price", 5000.0);
            case 6:
                return plugin.getRuntimeConfig().getDouble("level6-ghast.price", 10000.0);
            default:
                return 0;
        }
    }

    public int getFlightDuration(int level) {
        if (level == 6) {
            return plugin.getRuntimeConfig().getInt("level6-ghast.flight-duration", 30);
        }
        return 15;
    }

    public int getBufferDuration(int level) {
        return 5;
    }

    public String getPermission(int level) {
        switch (level) {
            case 1:
                return "ghastspeed.vipflight.1";
            case 2:
                return "ghastspeed.vipflight.2";
            case 3:
                return "ghastspeed.vipflight.3";
            case 4:
                return "ghastspeed.vipflight.4";
            case 5:
                return plugin.getRuntimeConfig().getString("level5-ghast.permission", "ghastspeed.vipflight.5");
            case 6:
                return plugin.getRuntimeConfig().getString("level6-ghast.permission", "ghastspeed.vipflight.6");
            default:
                return "";
        }
    }

    private void startFlightChecker() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getScheduler().run(plugin, scheduledTask -> {
                    if (!player.isValid()) {
                        return;
                    }
                    if (player.getVehicle() instanceof LivingEntity vehicle) {
                        if (!vehicle.isValid()) {
                            return;
                        }
                        int level = getVehicleLevel(vehicle);
                        if (level >= 1 && level <= 6) {
                            checkFlightStatus(player, vehicle, level);
                        }
                    }
                }, null);
            }
        }, 20L, 20L);
    }

    private void checkFlightStatus(Player player, LivingEntity vehicle, int level) {
        if (player.hasPermission(getPermission(level))) {
            return;
        }

        long fuelExpire = getFuelExpire(vehicle);
        if (fuelExpire == 0) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < fuelExpire) {
            return;
        }

        int bufferMinutes = getBufferDuration(level);
        long bufferEnd = fuelExpire + (bufferMinutes * 60 * 1000L);

        if (now < bufferEnd) {
            if (!isInBuffer(vehicle)) {
                setInBuffer(vehicle, true);
                speedHarness.removeSpeedEffect(vehicle);
                player.sendTitle(ChatColor.RED + "⚠️ 油箱已见底！", ChatColor.YELLOW + "请在5分钟内输入 /ghastpay 加油，否则会坠机爆炸！", 10, 70, 20);
            }
        } else {
            if (vehicle.getEquipment() != null) {
                vehicle.getEquipment().setItem(org.bukkit.inventory.EquipmentSlot.BODY, null);
                vehicle.getEquipment().setItem(org.bukkit.inventory.EquipmentSlot.CHEST, null);
                vehicle.getEquipment().setDropChance(org.bukkit.inventory.EquipmentSlot.BODY, 0.0f);
                vehicle.getEquipment().setDropChance(org.bukkit.inventory.EquipmentSlot.CHEST, 0.0f);
                vehicle.getEquipment().clear();
            }
            Location loc = vehicle.getLocation();
            vehicle.remove();
            loc.getWorld().createExplosion(loc, 4.0f, false, false);
            player.setHealth(0);
            tailNumberManager.logCrash(player, vehicle, "燃油耗尽");
            player.sendMessage(ChatColor.RED + "💥 轰！油量耗尽坠机！");
            speedHarness.removeSpeedEffect(vehicle);
            clearFlightData(vehicle);
        }
    }

    private ItemStack getEquippedHarness(LivingEntity entity) {
        var equipment = entity.getEquipment();
        if (equipment == null) {
            return null;
        }

        ItemStack item = equipment.getItem(org.bukkit.inventory.EquipmentSlot.BODY);
        if (item != null && item.getType().name().endsWith("_HARNESS")) {
            return item;
        }

        item = equipment.getItem(org.bukkit.inventory.EquipmentSlot.CHEST);
        if (item != null && item.getType().name().endsWith("_HARNESS")) {
            return item;
        }

        return null;
    }

    public void reloadConfig() {
        plugin.reloadPluginConfig();
    }

    public String getPlaneTypeName(int level) {
        switch (level) {
            case 6:
                return "干线客机";
            case 5:
                return "支线客机";
            case 4:
                return "直升机4型";
            case 3:
                return "直升机3型";
            case 2:
                return "直升机2型";
            case 1:
                return "直升机1型";
            default:
                return "未知机型";
        }
    }

    public void startHoverTask(LivingEntity vehicle, int level) {
        vehicle.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (!vehicle.isValid() || !vehicle.getPassengers().isEmpty()) {
                scheduledTask.cancel();
                return;
            }

            long fuelExpire = getFuelExpire(vehicle);
            if (fuelExpire == 0) {
                scheduledTask.cancel();
                return;
            }

            long now = System.currentTimeMillis();
            long bufferEnd = fuelExpire + (getBufferDuration(level) * 60 * 1000L);

            if (now >= bufferEnd) {
                if (vehicle.getEquipment() != null) {
                    vehicle.getEquipment().setItem(org.bukkit.inventory.EquipmentSlot.BODY, null);
                    vehicle.getEquipment().setItem(org.bukkit.inventory.EquipmentSlot.CHEST, null);
                    vehicle.getEquipment().setDropChance(org.bukkit.inventory.EquipmentSlot.BODY, 0.0f);
                    vehicle.getEquipment().setDropChance(org.bukkit.inventory.EquipmentSlot.CHEST, 0.0f);
                    vehicle.getEquipment().clear();
                }
                Location loc = vehicle.getLocation();
                String tailNumber = tailNumberManager.getTailNumber(vehicle);
                String planeType = getPlaneTypeName(level);

                vehicle.remove();
                boolean damageBlocks = plugin.getRuntimeConfig().getBoolean("collision-explosion-damages-blocks", false);
                loc.getWorld().createExplosion(loc, 4.0f, false, damageBlocks);

                String message = org.bukkit.ChatColor.RED + "💥 ⚠️ " + tailNumber + "号" + planeType + " 因 在空中悬浮过久燃油耗尽而坠毁！";
                plugin.getServer().broadcastMessage(message);

                clearFlightData(vehicle);
                scheduledTask.cancel();
            } else if (now >= fuelExpire) {
                if (!isInBuffer(vehicle)) {
                    setInBuffer(vehicle, true);
                }
                vehicle.getWorld().spawnParticle(org.bukkit.Particle.CAMPFIRE_COSY_SMOKE, vehicle.getLocation().add(0, 2, 0), 5, 0.5, 0.5, 0.5, 0.05);
            }
        }, null, 20L, 20L);
    }
}
