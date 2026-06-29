package com.ghastspeed;

import com.ghastspeed.commands.GhostSpeedCommand;
import com.ghastspeed.commands.GhastPayCommand;
import com.ghastspeed.commands.ReloadCommand;
import com.ghastspeed.flight.FlightSystemManager;
import com.ghastspeed.flight.TailNumberManager;
import com.ghastspeed.items.SpeedHarness;
import com.ghastspeed.listeners.HarnessListener;
import com.ghastspeed.listeners.VehicleEnterListener;
import com.ghastspeed.listeners.VehicleExitListener;
import com.ghastspeed.listeners.EndFlightRestrictionListener;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import com.ghastspeed.recipes.RecipeManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class GhostSpeed extends JavaPlugin {

    private static GhostSpeed instance;
    private SpeedHarness speedHarness;
    private RecipeManager recipeManager;
    private HarnessListener harnessListener;
    private FlightSystemManager flightSystemManager;
    private TailNumberManager tailNumberManager;
    private volatile boolean blockEndFlights;
    private volatile String endFlightMessage;
    private volatile FileConfiguration runtimeConfig;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        runtimeConfig = loadRuntimeConfig();
        loadRuntimeSettings();

        speedHarness = new SpeedHarness(this);
        recipeManager = new RecipeManager(this);
        tailNumberManager = new TailNumberManager(this);
        speedHarness.setTailNumberManager(tailNumberManager);
        harnessListener = new HarnessListener(this);
        flightSystemManager = new FlightSystemManager(this, tailNumberManager);

        recipeManager.registerRecipes();

        getServer().getPluginManager().registerEvents(new VehicleEnterListener(this, flightSystemManager, tailNumberManager), this);
        getServer().getPluginManager().registerEvents(new VehicleExitListener(this, flightSystemManager, tailNumberManager), this);
        getServer().getPluginManager().registerEvents(new EndFlightRestrictionListener(this), this);

        GhostSpeedCommand commandExecutor = new GhostSpeedCommand(this);
        this.getCommand("ghastspeed").setExecutor(commandExecutor);

        GhastPayCommand ghastPayCommand = new GhastPayCommand(this, flightSystemManager, tailNumberManager);
        this.getCommand("ghastpay").setExecutor(ghastPayCommand);

        ReloadCommand reloadCommand = new ReloadCommand(this);
        this.getCommand("ghastspeedreload").setExecutor(reloadCommand);

        getLogger().info("GhostSpeed 已成功加载!");
    }

    @Override
    public void onDisable() {
        if (tailNumberManager != null) {
            tailNumberManager.shutdown();
        }
        getLogger().info("GhostSpeed 已成功卸载!");
    }

    public static GhostSpeed getInstance() {
        return instance;
    }

    public SpeedHarness getSpeedHarness() {
        return speedHarness;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public FlightSystemManager getFlightSystemManager() {
        return flightSystemManager;
    }

    public HarnessListener getHarnessListener() {
        return harnessListener;
    }

    public TailNumberManager getTailNumberManager() {
        return tailNumberManager;
    }

    public void reloadPluginConfig() {
        runtimeConfig = loadRuntimeConfig();
        loadRuntimeSettings();
    }

    public FileConfiguration getRuntimeConfig() {
        return runtimeConfig;
    }

    public boolean isFlightBlocked(World world) {
        return blockEndFlights && world.getEnvironment() == World.Environment.THE_END;
    }

    public String getEndFlightMessage() {
        return endFlightMessage;
    }

    private void loadRuntimeSettings() {
        blockEndFlights = runtimeConfig.getBoolean("end-flight-restriction.enabled", true);
        endFlightMessage = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                runtimeConfig.getString("end-flight-restriction.message",
                        "&c末地空间裂隙与龙息会持续扰乱飞行器导航核心，禁止在此组装或启动飞机！"));
    }


    private FileConfiguration loadRuntimeConfig() {
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
    }
}
