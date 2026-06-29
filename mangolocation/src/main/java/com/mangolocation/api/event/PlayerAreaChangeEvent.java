package com.mangolocation.api.event;

import com.mangolocation.api.Area;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import java.util.Optional;

/** Fired when a player enters, leaves, or moves between configured areas. */
public final class PlayerAreaChangeEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Area fromArea;
    private final Area toArea;

    public PlayerAreaChangeEvent(Player player, Area fromArea, Area toArea) {
        super(player);
        this.fromArea = fromArea;
        this.toArea = toArea;
    }

    public Optional<Area> getFromArea() {
        return Optional.ofNullable(fromArea);
    }

    public Optional<Area> getToArea() {
        return Optional.ofNullable(toArea);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
