package com.ghastspeed.commands;

import com.ghastspeed.GhostSpeed;
import com.ghastspeed.flight.FlightSystemManager;
import com.ghastspeed.flight.TailNumberManager;
import com.ghastspeed.items.SpeedHarness;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class GhastPayCommand implements CommandExecutor {

    private final GhostSpeed plugin;
    private final FlightSystemManager flightManager;
    private final TailNumberManager tailNumberManager;
    private final SpeedHarness speedHarness;

    public GhastPayCommand(GhostSpeed plugin, FlightSystemManager flightManager, TailNumberManager tailNumberManager) {
        this.plugin = plugin;
        this.flightManager = flightManager;
        this.tailNumberManager = tailNumberManager;
        this.speedHarness = plugin.getSpeedHarness();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此指令！");
            return true;
        }

        if (!(player.getVehicle() instanceof LivingEntity vehicle)) {
            player.sendMessage(ChatColor.RED + "你必须正在骑乘快乐恶魂！");
            return true;
        }

        int level = flightManager.getVehicleLevel(vehicle);
        if (level < 1 || level > 6) {
            player.sendMessage(ChatColor.RED + "你必须正在骑乘1-6级快乐恶魂！");
            return true;
        }

        long fuelExpire = flightManager.getFuelExpire(vehicle);
        long now = System.currentTimeMillis();

        if (fuelExpire > now && !flightManager.isInBuffer(vehicle)) {
            player.sendMessage(ChatColor.YELLOW + "燃油充足，无需支付！");
            return true;
        }

        double price = flightManager.getPrice(level);
        int duration = flightManager.getFlightDuration(level);

        if (!flightManager.hasEconomy()) {
            player.sendMessage(ChatColor.RED + "经济系统未启用，无法扣费！");
            return true;
        }

        if (!flightManager.canAfford(player, price)) {
            player.sendMessage(ChatColor.RED + "你没有足够的钱！需要 " + price + " 元油钱！");
            return true;
        }

        if (!flightManager.withdrawMoney(player, price)) {
            player.sendMessage(ChatColor.RED + "扣费失败！");
            return true;
        }

        long newExpire = now + (duration * 60 * 1000L);
        flightManager.setFuelExpire(vehicle, newExpire);
        flightManager.setInBuffer(vehicle, false);
        flightManager.setCrashed(vehicle, false);

        vehicle.getScheduler().run(plugin, scheduledTask -> {
            if (!vehicle.isValid() || !player.isValid()) {
                return;
            }

            vehicle.setAI(true);
            speedHarness.applySpeedEffect(vehicle, getEquippedHarness(vehicle));
            tailNumberManager.logFlightTakeoff(player, vehicle);

            player.sendMessage(ChatColor.GREEN + "✈️ 支付成功！已缴纳 " + price + " 元油钱，启动发动机！获得 " + duration + " 分钟高速飞行！");
            if (level <= 4) {
                player.sendMessage(ChatColor.YELLOW + "⚠️ 请注意：直升机若未停靠在停机坪下机，将持续悬浮消耗燃油直至坠毁！");
            }
        }, null);

        return true;
    }

    private org.bukkit.inventory.ItemStack getEquippedHarness(LivingEntity entity) {
        var equipment = entity.getEquipment();
        if (equipment == null) {
            return null;
        }

        org.bukkit.inventory.ItemStack item = equipment.getItem(org.bukkit.inventory.EquipmentSlot.BODY);
        if (item != null && item.getType().name().endsWith("_HARNESS")) {
            return item;
        }

        item = equipment.getItem(org.bukkit.inventory.EquipmentSlot.CHEST);
        if (item != null && item.getType().name().endsWith("_HARNESS")) {
            return item;
        }

        return null;
    }
}
