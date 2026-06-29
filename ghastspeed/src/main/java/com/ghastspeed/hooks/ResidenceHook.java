package com.ghastspeed.hooks;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.ghastspeed.GhostSpeed;
import org.bukkit.Location;

import java.util.List;

public class ResidenceHook implements RegionHook {
    private final GhostSpeed plugin;

    public ResidenceHook(GhostSpeed plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isInAllowedRegion(Location loc, int level) {
        ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(loc);
        if (res == null) return false;

        String resName = res.getName();

        List<String> helicopterRegions = plugin.getRuntimeConfig().getStringList("helicopters.allowed-regions");
        List<String> level5Regions = plugin.getRuntimeConfig().getStringList("level5-ghast.allowed-regions");
        List<String> level6Regions = plugin.getRuntimeConfig().getStringList("level6-ghast.allowed-regions");

        if (level >= 1 && level <= 4) {
            return helicopterRegions.stream().anyMatch(name -> resName.toLowerCase().contains(name.toLowerCase())) ||
                   level5Regions.stream().anyMatch(name -> resName.toLowerCase().contains(name.toLowerCase())) ||
                   level6Regions.stream().anyMatch(name -> resName.toLowerCase().contains(name.toLowerCase()));
        } else if (level == 5) {
            return level5Regions.stream().anyMatch(name -> resName.toLowerCase().contains(name.toLowerCase())) ||
                   level6Regions.stream().anyMatch(name -> resName.toLowerCase().contains(name.toLowerCase()));
        } else if (level == 6) {
            return level6Regions.stream().anyMatch(name -> resName.toLowerCase().contains(name.toLowerCase()));
        }
        return false;
    }
}
