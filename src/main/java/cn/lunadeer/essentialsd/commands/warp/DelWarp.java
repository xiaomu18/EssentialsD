package cn.lunadeer.essentialsd.commands.warp;

import cn.lunadeer.essentialsd.dtos.WarpPoint;
import cn.lunadeer.minecraftpluginutils.Notification;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DelWarp implements TabExecutor {
   public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
      if (strings.length != 1) {
         Notification.error(commandSender, "用法: /delwarp <name>");
         return true;
      } else {
         WarpPoint point = WarpPoint.selectByName(strings[0]);
         if (point == null) {
            Notification.error(commandSender, "传送点 %s 不存在", strings[0]);
            return true;
         } else {
            WarpPoint.delete(point);
            Notification.info(commandSender, "传送点 %s 已删除", strings[0]);
            return true;
         }
      }
   }

   public @Nullable List onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      return WarpPoint.selectAllNames();
   }
}
