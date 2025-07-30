package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.minecraftpluginutils.Notification;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Suicide implements CommandExecutor {
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player)) {
         Notification.warn(sender, "只有玩家可以使用此命令");
         return true;
      } else {
         Player player = (Player)sender;
         player.setHealth((double)0.0F);
         player.setKiller(player);
         Notification.info(player, "你自杀了");
         EssentialsD.instance.getServer().broadcastMessage("§6" + player.getName() + " 结束了自己的生命.");
         return true;
      }
   }
}
