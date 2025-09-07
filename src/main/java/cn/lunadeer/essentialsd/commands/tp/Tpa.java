package cn.lunadeer.essentialsd.commands.tp;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.commands.Vanish;
import cn.lunadeer.minecraftpluginutils.Notification;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Tpa implements CommandExecutor {
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player)) {
         Notification.warn(sender, "只有玩家可以使用此命令");
         return true;
      } else {
         Player player = (Player)sender;
         if (args.length == 1) {
            Player target = EssentialsD.instance.getServer().getPlayer(args[0]);
            if (target == null || ((Vanish) EssentialsD.commands.get("vanish")).invList.contains(target.getUniqueId())) {
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
}
