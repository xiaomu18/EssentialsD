package cn.lunadeer.essentialsd.recipes;

import cn.lunadeer.essentialsd.EssentialsD;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class InvisibleItemFrame {
   public static ShapedRecipe getRecipe() {
      NamespacedKey key = new NamespacedKey(EssentialsD.instance, "invisible_item_frame");
      ItemStack item = getItemStack();
      ShapedRecipe recipe = new ShapedRecipe(key, item);
      recipe.shape(new String[]{"sss", "sgs", "sss"});
      recipe.setIngredient('s', Material.STICK);
      recipe.setIngredient('g', Material.GLASS_PANE);
      return recipe;
   }

   public static ItemStack getItemStack() {
      return getItemStack(1);
   }

   public static ItemStack getItemStack(Integer size) {
      ItemStack item = new ItemStack(Material.ITEM_FRAME, size);
      ItemMeta meta = item.getItemMeta();
      meta.displayName(Component.text("隐形物品展示框"));
      meta.getPersistentDataContainer().set(new NamespacedKey(EssentialsD.instance, "invisible"), PersistentDataType.BYTE, (byte)1);
      List<Component> lore = new ArrayList();
      lore.add(Component.text("放置物品后会自动隐形"));
      meta.lore(lore);
      item.setItemMeta(meta);
      return item;
   }
}
