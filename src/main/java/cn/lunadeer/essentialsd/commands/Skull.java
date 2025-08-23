package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.minecraftpluginutils.Notification;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

public class Skull implements CommandExecutor {
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player)) {
         Notification.warn(sender, "只有玩家可以使用此命令");
         return true;
      } else {
         Player player = (Player)sender;
         PlayerInventory backpack = player.getInventory();
         Map<Integer, ? extends ItemStack> creeper_skulls = backpack.all(Material.CREEPER_HEAD);
         Map<Integer, ? extends ItemStack> skeleton_skulls = backpack.all(Material.SKELETON_SKULL);
         Map<Integer, ? extends ItemStack> zombie_skulls = backpack.all(Material.ZOMBIE_HEAD);
         Map<Integer, ? extends ItemStack> dragon_skulls = backpack.all(Material.DRAGON_HEAD);
         Map<Integer, ? extends ItemStack> wither_skulls = backpack.all(Material.WITHER_SKELETON_SKULL);
         HashMap<Integer, ItemStack> skulls = new HashMap();
         skulls.putAll(creeper_skulls);
         skulls.putAll(skeleton_skulls);
         skulls.putAll(zombie_skulls);
         skulls.putAll(dragon_skulls);
         skulls.putAll(wither_skulls);
         if (skulls.isEmpty()) {
            Notification.warn(player, "你的背包中没有可以用来交换的头颅");
            return true;
         } else {
            ItemStack skull = ((ItemStack[])skulls.values().toArray(new ItemStack[0]))[0];
            if (skull.getAmount() > 1) {
               skull.setAmount(skull.getAmount() - 1);
            } else {
               backpack.setItem(((Integer[])skulls.keySet().toArray(new Integer[0]))[0], (ItemStack)null);
            }

            ItemStack player_skull = getPlayerSkull(player);
            player.getWorld().dropItem(player.getLocation(), player_skull);
            Notification.info(player, "交换成功");
            return true;
         }
      }
   }

   public static ItemStack getPlayerSkull(Player player) {
      ItemStack head = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta headMeta = (SkullMeta)head.getItemMeta();
      headMeta.setOwningPlayer(player);
      head.setItemMeta(headMeta);
      return head;
   }
}
