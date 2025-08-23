package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.minecraftpluginutils.Notification;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

public class EnderChest implements CommandExecutor {
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player)) {
         Notification.error(sender, "只有玩家才能使用这个命令");
         return false;
      } else {
         Player player = (Player)sender;
         Inventory chest = player.getEnderChest();
         InventoryView view = player.openInventory(chest);
         if (view == null) {
            Notification.error(sender, "无法打开末影箱");
            return false;
         } else {
            InventoryOpenEvent event = new InventoryOpenEvent(view);
            EssentialsD.instance.getServer().getPluginManager().callEvent(event);
            return true;
         }
      }
   }
}
