package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.minecraftpluginutils.Notification;
import cn.lunadeer.minecraftpluginutils.Scheduler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class God implements CommandExecutor {
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player) && args.length < 1) {
         Notification.error(sender, "以控制台身份执行时，必须指定玩家：/god <player>");
         return false;
      } else {
         Scheduler.runTask(() -> {
            Player target = Apis.getPlayerFromArg(sender, args, 0);
            if (target != null) {
               if (target.isInvulnerable()) {
                  target.setInvulnerable(false);
                  Notification.info(sender, "已关闭玩家 %s 的无敌模式", target.getName());
                  Notification.info(target, "已关闭无敌模式");
               } else {
                  target.setInvulnerable(true);
                  Notification.info(sender, "已开启玩家 %s 的无敌模式", target.getName());
                  Notification.info(target, "已开启无敌模式");
               }

            }
         });
         return true;
      }
   }
}
