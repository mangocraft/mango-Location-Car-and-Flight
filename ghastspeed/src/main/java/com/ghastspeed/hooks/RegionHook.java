package com.ghastspeed.hooks;

import org.bukkit.Location;

public interface RegionHook {
    boolean isInAllowedRegion(Location loc, int level);
}
