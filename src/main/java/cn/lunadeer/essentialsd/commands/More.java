package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.minecraftpluginutils.Notification;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class More implements CommandExecutor {
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player)) {
         Notification.warn(sender, "只有玩家可以使用此命令");
         return true;
      } else {
         Player player = (Player)sender;
         int amount = 63;
         if (args.length > 0) {
            try {
               amount = Integer.parseInt(args[0]);
            } catch (NumberFormatException var11) {
               Notification.error(sender, "参数错误");
               return false;
            }
         }

         ItemStack item = player.getInventory().getItemInMainHand();
         if (item.getType().isAir()) {
            Notification.error(sender, "你手上没有物品");
            return false;
         } else {
            int failed_add_count = 0;

            for(int i = 0; i < amount; ++i) {
               Map<Integer, ItemStack> res = player.getInventory().addItem(new ItemStack[]{new ItemStack(item.getType())});
               if (!res.isEmpty()) {
                  ++failed_add_count;
               }
            }

            if (failed_add_count != 0) {
               Notification.warn(sender, "背包已满，有 %d 个物品未能添加", failed_add_count);
            }

            return true;
         }
      }
   }
}
