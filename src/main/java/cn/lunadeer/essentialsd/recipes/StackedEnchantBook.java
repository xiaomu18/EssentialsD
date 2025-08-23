package cn.lunadeer.essentialsd.recipes;

import cn.lunadeer.essentialsd.EssentialsD;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

public class StackedEnchantBook {
   public static ShapelessRecipe getRecipe() {
      NamespacedKey key = new NamespacedKey(EssentialsD.instance, "two_curse_of_vanishing_books");
      ItemStack item = stackedEnchantBook();
      ShapelessRecipe recipe = new ShapelessRecipe(key, item);
      recipe.addIngredient(Material.BOOK);
      recipe.addIngredient(Material.BOOK);
      return recipe;
   }

   public static ItemStack stackedEnchantBook() {
      return new ItemStack(Material.ENCHANTED_BOOK, 2);
   }
}
