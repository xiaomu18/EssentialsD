package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.minecraftpluginutils.Notification;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

public class Hat implements CommandExecutor {
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player)) {
         Notification.warn(sender, "只有玩家可以使用此命令");
         return true;
      } else {
         Player player = (Player)sender;
         PlayerInventory inventory = player.getInventory();
         ItemStack right_hand_item = inventory.getItemInMainHand();
         if (right_hand_item.getType().isAir()) {
            Notification.warn(player, "你的主手为空");
            return true;
         } else {
            ItemStack head_item = inventory.getHelmet();
            inventory.setItemInMainHand(head_item);
            inventory.setHelmet(right_hand_item);
            Notification.info(player, "已将你的主物品放到了你的头上");
            return true;
         }
      }
   }
}
