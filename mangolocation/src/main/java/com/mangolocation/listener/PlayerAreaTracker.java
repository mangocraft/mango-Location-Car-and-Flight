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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerAreaTracker implements Listener {

    private final MangoLocation plugin;
    private final ConfiguredAreaService api;
    private final Map<UUID, TrackedArea> currentAreas = new ConcurrentHashMap<>();

    public PlayerAreaTracker(MangoLocation plugin, ConfiguredAreaService api) {
        this.plugin = plugin;
        this.api = api;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        update(event.getPlayer(), event.getPlayer().getLocation(), true);
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
        TrackedArea previous = currentAreas.get(player.getUniqueId());
        String currentWorld = location.getWorld() == null ? null : location.getWorld().getName();
        Area currentArea = api.findArea(location).orElse(null);
        TrackedArea current = currentArea == null || currentWorld == null
                ? null : new TrackedArea(currentArea, currentWorld);
        if (sameArea(previous, current)) {
            // A reload can replace an Area while keeping its ID. Retain the new immutable value.
            if (current != null && current.area() != previous.area()) {
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
        plugin.getServer().getPluginManager().callEvent(new PlayerAreaChangeEvent(
                player,
                previous == null ? null : previous.area(), previous == null ? null : previous.worldName(),
                current == null ? null : current.area(), current == null ? null : current.worldName()));
        notifyPlayer(player, previous, current);
    }

    private boolean sameArea(TrackedArea first, TrackedArea second) {
        return first == second || (first != null && second != null
                && first.area().id().equals(second.area().id())
                && first.worldName().equals(second.worldName()));
    }

    private void notifyPlayer(Player player, TrackedArea previous, TrackedArea current) {
        if (!api.shouldNotifyPlayer()) {
            return;
        }

        if (previous != null && api.isMainWorld(previous.worldName())) {
            sendLines(player, api.getLeaveMessages(previous.area()));
        }
        if (current != null && api.isMainWorld(current.worldName())) {
            sendLines(player, api.getEnterMessages(current.area()));
        }

        String message;
        if (current != null && api.isMainWorld(current.worldName())) {
            message = api.formatEnterMessage(current.area());
        } else if (previous != null && api.isMainWorld(previous.worldName())) {
            message = api.formatLeaveMessage(previous.area());
        } else {
            return;
        }
        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }

    private void sendLines(Player player, java.util.List<String> lines) {
        lines.forEach(line -> player.sendMessage(
                LegacyComponentSerializer.legacyAmpersand().deserialize(line)));
    }

    private record TrackedArea(Area area, String worldName) {
    }
}
