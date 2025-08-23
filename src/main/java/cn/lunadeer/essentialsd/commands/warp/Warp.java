package cn.lunadeer.essentialsd.commands.warp;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.dtos.WarpPoint;
import cn.lunadeer.minecraftpluginutils.Notification;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Warp implements TabExecutor {
   public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
      if (!(commandSender instanceof Player)) {
         Notification.warn(commandSender, "只有玩家可以使用此命令");
         return true;
      } else {
         Player player = (Player)commandSender;
         if (strings.length != 1) {
            Notification.error(commandSender, "用法: /warp <name>");
            return true;
         } else {
            WarpPoint point = WarpPoint.selectByName(strings[0]);
            if (point == null) {
               Notification.error(commandSender, "传送点 %s 不存在", strings[0]);
               return true;
            } else {
               try {
                  EssentialsD.tpManager.doTeleportDelayed(player, point.getLocation(), (Integer)EssentialsD.config.getTpDelay(), () -> Notification.info(player, "正在传送到 %s", strings[0]), () -> Notification.info(player, "已传送到 %s", strings[0]));
               } catch (RuntimeException e) {
                  Notification.error(player, e.getMessage());
               }

               return true;
            }
         }
      }
   }

   public @Nullable List onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      return WarpPoint.selectAllNames();
   }
}
