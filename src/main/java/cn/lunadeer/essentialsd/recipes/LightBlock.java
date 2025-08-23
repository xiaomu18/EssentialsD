package cn.lunadeer.essentialsd.recipes;

import cn.lunadeer.essentialsd.EssentialsD;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

public class LightBlock {
   public static ShapelessRecipe getRecipe() {
      NamespacedKey key = new NamespacedKey(EssentialsD.instance, "torch_to_light_block");
      ItemStack item = new ItemStack(Material.LIGHT, 1);
      ShapelessRecipe recipe = new ShapelessRecipe(key, item);
      recipe.addIngredient(Material.TORCH);
      return recipe;
   }
}
