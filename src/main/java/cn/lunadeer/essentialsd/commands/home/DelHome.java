package cn.lunadeer.essentialsd.commands.home;

import cn.lunadeer.essentialsd.dtos.HomeInfo;
import cn.lunadeer.essentialsd.tuis.HomeList;
import cn.lunadeer.minecraftpluginutils.Notification;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DelHome implements TabExecutor {
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player)) {
         Notification.warn(sender, "只有玩家可以使用此命令");
         return true;
      } else {
         Player player = (Player)sender;
         String homeName;
         if (args.length == 0) {
            homeName = "default";
         } else {
            homeName = args[0];
         }

         boolean res = HomeInfo.deleteHome(player.getUniqueId(), homeName);
         if (res) {
            Notification.info(player, "成功删除家 %s", homeName);
         } else {
            Notification.error(player, "删除家 %s 失败, 请联系管理员", homeName);
         }

         if (args.length == 2) {
            try {
               int page = Integer.parseInt(args[1]);
               String[] newArgs = new String[1];
               newArgs[0] = String.valueOf(page);
               HomeList.show(sender, newArgs);
            } catch (Exception var10) {
            }
         }

         return true;
      }
   }

   public @Nullable List onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      List<HomeInfo> homes = HomeInfo.getHomesOf(((Player)sender).getUniqueId());
      List<String> res = new ArrayList();

      for(HomeInfo home : homes) {
         res.add(home.homeName);
      }

      return res;
   }
}
