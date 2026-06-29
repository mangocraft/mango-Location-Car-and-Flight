package com.bmwspeed;

import com.bmwspeed.commands.AddGasCommand;
import com.bmwspeed.commands.CarSpeedCommand;
import com.bmwspeed.commands.ReloadCommand;
import com.bmwspeed.drive.DriveSystemManager;
import com.bmwspeed.drive.LicensePlateManager;
import com.bmwspeed.items.SpeedEngine;
import com.bmwspeed.listeners.VehicleEnterListener;
import com.bmwspeed.listeners.VehicleExitListener;
import com.bmwspeed.recipes.RecipeManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class BmwSpeed extends JavaPlugin {

    private static BmwSpeed instance;
    private SpeedEngine speedEngine;
    private RecipeManager recipeManager;
    private DriveSystemManager driveSystemManager;
    private LicensePlateManager licensePlateManager;
    private volatile FileConfiguration runtimeConfig;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        runtimeConfig = loadRuntimeConfig();

        speedEngine = new SpeedEngine(this);
        recipeManager = new RecipeManager(this);
        licensePlateManager = new LicensePlateManager(this);
        speedEngine.setLicensePlateManager(licensePlateManager);
        driveSystemManager = new DriveSystemManager(this, licensePlateManager);

        recipeManager.registerRecipes();

        getServer().getPluginManager().registerEvents(new VehicleEnterListener(this, driveSystemManager, licensePlateManager), this);
        getServer().getPluginManager().registerEvents(new VehicleExitListener(this, driveSystemManager, licensePlateManager), this);

        CarSpeedCommand commandExecutor = new CarSpeedCommand(this);
        this.getCommand("carspeed").setExecutor(commandExecutor);

        AddGasCommand addGasCommand = new AddGasCommand(this, driveSystemManager, licensePlateManager);
        this.getCommand("addgas").setExecutor(addGasCommand);

        ReloadCommand reloadCommand = new ReloadCommand(this);
        this.getCommand("bmwspeedreload").setExecutor(reloadCommand);

        getLogger().info("BmwSpeed 已成功加载！宝马跑车系统启动！");
    }

    @Override
    public void onDisable() {
        if (licensePlateManager != null) {
            licensePlateManager.shutdown();
        }
        getLogger().info("BmwSpeed 已成功卸载！");
    }

    public static BmwSpeed getInstance() {
        return instance;
    }

    public SpeedEngine getSpeedEngine() {
        return speedEngine;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public DriveSystemManager getDriveSystemManager() {
        return driveSystemManager;
    }

    public LicensePlateManager getLicensePlateManager() {
        return licensePlateManager;
    }

    public FileConfiguration getRuntimeConfig() {
        return runtimeConfig;
    }

    public void reloadPluginConfig() {
        runtimeConfig = loadRuntimeConfig();
    }

    private FileConfiguration loadRuntimeConfig() {
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
    }
}
