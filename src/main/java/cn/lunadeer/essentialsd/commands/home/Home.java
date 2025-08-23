package cn.lunadeer.essentialsd.commands.home;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.dtos.HomeInfo;
import cn.lunadeer.minecraftpluginutils.Notification;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Home implements TabExecutor {
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player)) {
         Notification.warn(sender, "只有玩家可以使用此命令");
         return true;
      } else {
         Player player = (Player)sender;
         List<HomeInfo> homes = HomeInfo.getHomesOf(player.getUniqueId());
         String homeName;
         if (args.length == 0) {
            if (homes.isEmpty()) {
               Notification.error(player, "你还没有设置家");
               return true;
            }

            homeName = ((HomeInfo)homes.get(0)).homeName;
         } else {
            homeName = args[0];
         }

         HomeInfo home = HomeInfo.getHome(player.getUniqueId(), homeName);
         if (home == null) {
            Notification.error(player, "不存在名为 %s 的家", homeName);
            return true;
         } else {
            try {
               EssentialsD.tpManager.doTeleportDelayed(player, home.location, (Integer)EssentialsD.config.getTpDelay(), () -> Notification.info(player, "正在传送到家 %s", homeName), () -> Notification.info(player, "成功传送到家 %s", homeName));
            } catch (RuntimeException e) {
               Notification.error(player, "传送到家 %s 失败: %s", homeName, e.getMessage());
            }

            return true;
         }
      }
   }

   public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      List<HomeInfo> homes = HomeInfo.getHomesOf(((Player)sender).getUniqueId());
      List<String> res = new ArrayList<>();

      for(HomeInfo home : homes) {
         res.add(home.homeName);
      }

      return res;
   }
}
