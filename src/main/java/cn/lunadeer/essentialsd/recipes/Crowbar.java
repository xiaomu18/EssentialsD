package cn.lunadeer.essentialsd.recipes;

import cn.lunadeer.essentialsd.EssentialsD;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.Rail.Shape;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class Crowbar {
   public static ShapelessRecipe getRecipe() {
      NamespacedKey key = new NamespacedKey(EssentialsD.instance, "crowbar");
      ItemStack item = getItemStack();
      ShapelessRecipe recipe = new ShapelessRecipe(key, item);
      recipe.addIngredient(Material.STICK);
      recipe.addIngredient(Material.IRON_INGOT);
      return recipe;
   }

   public static ItemStack getItemStack() {
      return getItemStack(1);
   }

   public static ItemStack getItemStack(Integer size) {
      ItemStack item = new ItemStack(Material.STICK, size);
      ItemMeta meta = item.getItemMeta();
      meta.displayName(Component.text("撬棍"));
      meta.getPersistentDataContainer().set(new NamespacedKey(EssentialsD.instance, "this_is_crowbar"), PersistentDataType.BYTE, (byte)1);
      List<Component> lore = new ArrayList();
      lore.add(Component.text("神奇的物理学圣剑"));
      lore.add(Component.text("对着铁轨使用可以改变铁轨方向"));
      meta.lore(lore);
      item.setItemMeta(meta);
      return item;
   }

   public static Rail.Shape changeToNext(Rail.Shape shape) {
      boolean found = false;

      for(Rail.Shape s : changeableShapes()) {
         if (found) {
            return s;
         }

         if (s == shape) {
            found = true;
         }
      }

      return Shape.NORTH_SOUTH;
   }

   public static Boolean changeable(Rail.Shape shape) {
      return changeableShapes().contains(shape);
   }

   public static List<Rail.Shape> changeableShapes() {
      List<Rail.Shape> shapes = new ArrayList();
      shapes.add(Shape.NORTH_SOUTH);
      shapes.add(Shape.EAST_WEST);
      shapes.add(Shape.NORTH_WEST);
      shapes.add(Shape.NORTH_EAST);
      shapes.add(Shape.SOUTH_EAST);
      shapes.add(Shape.SOUTH_WEST);
      return shapes;
   }
}
