package com.mangolocation.api.event;

import com.mangolocation.api.Area;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import java.util.Optional;

/** Fired when a player enters, leaves, or moves between configured areas or worlds. */
public final class PlayerAreaChangeEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Area fromArea;
    private final Area toArea;
    private final String fromWorldName;
    private final String toWorldName;

    public PlayerAreaChangeEvent(Player player, Area fromArea, Area toArea) {
        this(player, fromArea, player.getWorld().getName(), toArea, player.getWorld().getName());
    }

    public PlayerAreaChangeEvent(Player player, Area fromArea, String fromWorldName,
                                 Area toArea, String toWorldName) {
        super(player);
        this.fromArea = fromArea;
        this.toArea = toArea;
        this.fromWorldName = fromWorldName;
        this.toWorldName = toWorldName;
    }

    public Optional<Area> getFromArea() {
        return Optional.ofNullable(fromArea);
    }

    public Optional<Area> getToArea() {
        return Optional.ofNullable(toArea);
    }

    public Optional<String> getFromWorldName() {
        return Optional.ofNullable(fromWorldName);
    }

    public Optional<String> getToWorldName() {
        return Optional.ofNullable(toWorldName);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
