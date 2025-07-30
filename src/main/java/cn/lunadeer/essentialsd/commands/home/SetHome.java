package cn.lunadeer.essentialsd.commands.home;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.dtos.HomeInfo;
import cn.lunadeer.minecraftpluginutils.Notification;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SetHome implements TabExecutor {
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player)) {
         Notification.warn(sender, "只有玩家可以使用此命令");
         return true;
      } else {
         Player player = (Player)sender;
         List<HomeInfo> homes = HomeInfo.getHomesOf(((Player)sender).getUniqueId());
         if (homes.size() > EssentialsD.config.getHomeLimitAmount()) {
            Notification.error(player, "你的家数量已达上限");
            return true;
         } else {
            HomeInfo info = new HomeInfo();
            info.uuid = player.getUniqueId();
            if (args.length == 0) {
               info.homeName = "default";
            } else {
               info.homeName = args[0];
            }

            info.location = player.getLocation();
            HomeInfo exist = HomeInfo.getHome(player.getUniqueId(), info.homeName);
            if (exist != null) {
               Notification.error(player, "已经存在名为 %s 的家", info.homeName);
               return true;
            } else {
               boolean res = HomeInfo.newHome(info);
               if (res) {
                  Notification.info(player, "成功设置家 %s", info.homeName);
               } else {
                  Notification.error(player, "设置家 %s 失败, 请联系管理员", info.homeName);
               }

               return true;
            }
         }
      }
   }

   public @Nullable List onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      return args.length == 1 ? Collections.singletonList("[home名称]") : null;
   }
}
