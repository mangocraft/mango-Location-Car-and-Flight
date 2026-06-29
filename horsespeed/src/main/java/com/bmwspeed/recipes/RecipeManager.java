package com.bmwspeed.recipes;

import com.bmwspeed.BmwSpeed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class RecipeManager {

    private final BmwSpeed plugin;
    private final Map<Integer, Material> recipeMaterials;

    public RecipeManager(BmwSpeed plugin) {
        this.plugin = plugin;
        this.recipeMaterials = new HashMap<>();
        loadRecipeMaterials();
    }

    private void loadRecipeMaterials() {
        recipeMaterials.put(1, Material.valueOf(plugin.getRuntimeConfig().getString("recipe-materials.1", "GOLD_BLOCK")));
        recipeMaterials.put(2, Material.valueOf(plugin.getRuntimeConfig().getString("recipe-materials.2", "IRON_BLOCK")));
        recipeMaterials.put(3, Material.valueOf(plugin.getRuntimeConfig().getString("recipe-materials.3", "EMERALD_BLOCK")));
        recipeMaterials.put(4, Material.valueOf(plugin.getRuntimeConfig().getString("recipe-materials.4", "DIAMOND_BLOCK")));
        recipeMaterials.put(5, Material.valueOf(plugin.getRuntimeConfig().getString("recipe-materials.5", "NETHERITE_BLOCK")));
        recipeMaterials.put(6, Material.valueOf(plugin.getRuntimeConfig().getString("recipe-materials.6", "NETHERITE_BLOCK")));
    }

    public void registerRecipes() {
        for (int level = 1; level <= 6; level++) {
            for (String color : new String[]{"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"}) {
                registerRecipe(level, color);
            }
        }
    }

    private void registerRecipe(int level, String color) {
        Material baseMaterial = recipeMaterials.get(level);
        if (baseMaterial == null) {
            return;
        }

        Material woolMaterial = Material.valueOf(color.toUpperCase() + "_WOOL");
        ItemStack result = plugin.getSpeedEngine().createSpeedEngine(level, color, woolMaterial);

        NamespacedKey key = new NamespacedKey(plugin, "bmwspeed_" + level + "_" + color);
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        if (level >= 5) {
            // 豪华性能车 (M5, M8) 配方：加入下界之星作为 V8 核心动力
            recipe.shape(
                "BCB",
                "NSN",
                "RBR"
            );
            recipe.setIngredient('N', Material.NETHER_STAR);
        } else {
            // 普通家用轿车 (1-4 系) 配方：使用铁锭作为普通气缸
            recipe.shape(
                "BCB",
                "ISI",
                "RBR"
            );
            recipe.setIngredient('I', Material.IRON_INGOT);
        }

        // 公共材料：B=基础矿物块，C=羊毛 (车漆), S=马鞍 (方向盘/控制核心), R=红石 (电路)
        recipe.setIngredient('B', baseMaterial);
        recipe.setIngredient('C', woolMaterial);
        recipe.setIngredient('S', Material.SADDLE);
        recipe.setIngredient('R', Material.REDSTONE);

        plugin.getServer().addRecipe(recipe);
    }
}
