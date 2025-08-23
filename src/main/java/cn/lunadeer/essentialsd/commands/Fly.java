package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.minecraftpluginutils.Notification;
import cn.lunadeer.minecraftpluginutils.Scheduler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Fly implements CommandExecutor {
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player) && args.length == 0) {
         Notification.error(sender, "以控制台身份执行时，必须指定玩家：/fly <player>");
         return false;
      } else {
         if (!sender.hasPermission("essd.fly.other") && args.length > 0) {
            Notification.error(sender, "你无权调控他人的飞行模式");
            return false;
         }

         Scheduler.runTask(() -> {
            Player target = Apis.getPlayerFromArg(sender, args, 0);
            if (target != null) {
               if (target.getAllowFlight()) {
                  target.setAllowFlight(false);
                  Notification.info(sender, "已关闭玩家 %s 的飞行模式", target.getName());
                  Notification.info(target, "已关闭飞行模式");
               } else {
                  target.setAllowFlight(true);
                  Notification.info(sender, "已开启玩家 %s 的飞行模式", target.getName());
                  Notification.info(target, "已开启飞行模式");
               }

            }
         });
         return true;
      }
   }
}
