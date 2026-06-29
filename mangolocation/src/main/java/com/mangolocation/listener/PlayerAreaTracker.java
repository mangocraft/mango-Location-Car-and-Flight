package com.mangolocation.listener;

import com.mangolocation.MangoLocation;
import com.mangolocation.api.Area;
import com.mangolocation.api.event.PlayerAreaChangeEvent;
import com.mangolocation.internal.ConfiguredAreaService;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerAreaTracker implements Listener {

    private final MangoLocation plugin;
    private final ConfiguredAreaService api;
    private final Map<UUID, Area> currentAreas = new ConcurrentHashMap<>();

    public PlayerAreaTracker(MangoLocation plugin, ConfiguredAreaService api) {
        this.plugin = plugin;
        this.api = api;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        update(event.getPlayer(), event.getPlayer().getLocation(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() == to.getWorld()
                && from.getBlockX() == to.getBlockX()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        update(event.getPlayer(), to, true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        player.getScheduler().run(plugin, task -> update(player, player.getLocation(), true), null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        currentAreas.remove(event.getPlayer().getUniqueId());
    }

    public void refresh(Player player) {
        update(player, player.getLocation(), true);
    }

    private void update(Player player, Location location, boolean fireEvent) {
        Area previous = currentAreas.get(player.getUniqueId());
        Area current = api.findArea(location).orElse(null);
        if (sameArea(previous, current)) {
            // A reload can replace an Area while keeping its ID. Retain the new immutable value.
            if (current != null && current != previous) {
                currentAreas.put(player.getUniqueId(), current);
            }
            return;
        }

        if (current == null) {
            currentAreas.remove(player.getUniqueId());
        } else {
            currentAreas.put(player.getUniqueId(), current);
        }

        if (!fireEvent) {
            return;
        }
        plugin.getServer().getPluginManager().callEvent(new PlayerAreaChangeEvent(player, previous, current));
        notifyPlayer(player, previous, current);
    }

    private boolean sameArea(Area first, Area second) {
        return first == second || (first != null && second != null && Objects.equals(first.id(), second.id()));
    }

    private void notifyPlayer(Player player, Area previous, Area current) {
        if (!api.shouldNotifyPlayer()) {
            return;
        }

        String message;
        Area messageArea;
        if (current != null) {
            messageArea = current;
            message = api.formatEnterMessage(messageArea);
        } else if (previous != null) {
            messageArea = previous;
            message = api.formatLeaveMessage(messageArea);
        } else {
            return;
        }
        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }
}
