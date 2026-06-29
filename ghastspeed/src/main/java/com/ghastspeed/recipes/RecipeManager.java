package com.ghastspeed.recipes;

import com.ghastspeed.GhostSpeed;
import com.ghastspeed.items.SpeedHarness;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.HashMap;
import java.util.Map;

public class RecipeManager {

    private final GhostSpeed plugin;
    private final SpeedHarness speedHarness;
    private final Map<Integer, Material> levelMaterials;
    private final Map<Material, String> woolColors;

    public RecipeManager(GhostSpeed plugin) {
        this.plugin = plugin;
        this.speedHarness = plugin.getSpeedHarness();
        this.levelMaterials = new HashMap<>();
        this.woolColors = new HashMap<>();
        initializeMaterials();
        initializeWoolColors();
    }

    private void initializeMaterials() {
        levelMaterials.put(1, Material.valueOf(plugin.getRuntimeConfig().getString("recipe-materials.1", "GOLD_BLOCK")));
        levelMaterials.put(2, Material.valueOf(plugin.getRuntimeConfig().getString("recipe-materials.2", "IRON_BLOCK")));
        levelMaterials.put(3, Material.valueOf(plugin.getRuntimeConfig().getString("recipe-materials.3", "EMERALD_BLOCK")));
        levelMaterials.put(4, Material.valueOf(plugin.getRuntimeConfig().getString("recipe-materials.4", "DIAMOND_BLOCK")));
        levelMaterials.put(5, Material.valueOf(plugin.getRuntimeConfig().getString("recipe-materials.5", "NETHERITE_BLOCK")));
    }

    private void initializeWoolColors() {
        woolColors.put(Material.WHITE_WOOL, "白色");
        woolColors.put(Material.ORANGE_WOOL, "橙色");
        woolColors.put(Material.MAGENTA_WOOL, "品红色");
        woolColors.put(Material.LIGHT_BLUE_WOOL, "淡蓝色");
        woolColors.put(Material.YELLOW_WOOL, "黄色");
        woolColors.put(Material.LIME_WOOL, "黄绿色");
        woolColors.put(Material.PINK_WOOL, "粉红色");
        woolColors.put(Material.GRAY_WOOL, "灰色");
        woolColors.put(Material.LIGHT_GRAY_WOOL, "淡灰色");
        woolColors.put(Material.CYAN_WOOL, "青色");
        woolColors.put(Material.PURPLE_WOOL, "紫色");
        woolColors.put(Material.BLUE_WOOL, "蓝色");
        woolColors.put(Material.BROWN_WOOL, "棕色");
        woolColors.put(Material.GREEN_WOOL, "绿色");
        woolColors.put(Material.RED_WOOL, "红色");
        woolColors.put(Material.BLACK_WOOL, "黑色");
    }

    public void registerRecipes() {
        for (int level = 1; level <= 5; level++) {
            for (Map.Entry<Material, String> entry : woolColors.entrySet()) {
                registerRecipe(level, entry.getKey(), entry.getValue());
            }
        }
    }

    private void registerRecipe(int level, Material woolMaterial, String colorName) {
        Material speedMaterial = levelMaterials.get(level);
        if (speedMaterial == null) {
            return;
        }

        ItemStack result = speedHarness.createSpeedHarness(level, colorName, woolMaterial);

        NamespacedKey key = new NamespacedKey(plugin, "speed_harness_" + level + "_" + woolMaterial.name().toLowerCase());
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        recipe.shape(
                "LLL",
                "GWG",
                "SSS"
        );

        recipe.setIngredient('L', Material.LEATHER);
        recipe.setIngredient('G', Material.DIAMOND_BLOCK);
        recipe.setIngredient('W', woolMaterial);
        recipe.setIngredient('S', speedMaterial);

        plugin.getServer().addRecipe(recipe);
    }

    public Material getLevelMaterial(int level) {
        return levelMaterials.get(level);
    }

    public String getWoolColorName(Material wool) {
        return woolColors.get(wool);
    }
}
