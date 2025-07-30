package cn.lunadeer.essentialsd.commands.tp;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.minecraftpluginutils.Notification;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Tpa implements TabExecutor {
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player)) {
         Notification.warn(sender, "只有玩家可以使用此命令");
         return true;
      } else {
         Player player = (Player)sender;
         if (args.length == 1) {
            Player target = EssentialsD.instance.getServer().getPlayer(args[0]);
            if (target == null) {
               Notification.warn(player, "玩家 %s 不在线", args[0]);
               return true;
            } else {
               EssentialsD.tpManager.tpaRequest(player, target);
               return true;
            }
         } else if (args.length == 2) {
            if (args[0].equals("accept")) {
               EssentialsD.tpManager.accept(player, UUID.fromString(args[1]));
               return true;
            } else if (args[0].equals("deny")) {
               EssentialsD.tpManager.deny(player, UUID.fromString(args[1]));
               return true;
            } else {
               Notification.error(player, "参数错误");
               return false;
            }
         } else {
            Notification.error(player, "参数错误");
            return false;
         }
      }
   }

   public @Nullable List onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (args.length != 1) {
         return null;
      } else {
         Collection<? extends Player> players = EssentialsD.instance.getServer().getOnlinePlayers();
         List<String> result = new ArrayList();

         for(Player player : players) {
            result.add(player.getName());
         }

         return result;
      }
   }
}
