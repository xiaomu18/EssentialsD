package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.minecraftpluginutils.Notification;
import cn.lunadeer.minecraftpluginutils.Scheduler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class Inspect implements CommandExecutor {
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player)) {
         Notification.error(sender, "只有玩家才能使用这个命令");
         return false;

      } else if (command.getName().equals("inspect")) {
         Player op = (Player)sender;
         Scheduler.runTask(() -> {
            Player target = Apis.getPlayerFromArg(op, args, 0);
            if (target != null) {
               Inventory inv = target.getInventory();
               op.openInventory(inv);
            }
         });
         return true;

      } else if (command.getName().equals("inspect-ender")) {
         Player op = (Player)sender;
         Scheduler.runTask(() -> {
            Player target = Apis.getPlayerFromArg(op, args, 0);
            if (target != null) {
               Inventory inv = target.getEnderChest();
               op.openInventory(inv);
            }
         });
         return true;
      }

      return false;
   }
}
