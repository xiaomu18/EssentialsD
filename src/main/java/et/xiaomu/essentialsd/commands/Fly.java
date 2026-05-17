package et.xiaomu.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Fly implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player) && args.length == 0) {
            Notification.errorKey(sender, "messages.fly.console_requires_player");
            return false;
        } else {
            if (!sender.hasPermission("essd.fly.other") && args.length > 0) {
                Notification.errorKey(sender, "messages.fly.no_permission_other");
                return false;
            }

            Scheduler.runTask(() -> {
                Player target = Apis.getPlayerFromArg(sender, args, 0);
                if (target != null) {
                    if (target.getAllowFlight()) {
                        target.setAllowFlight(false);
                        Notification.infoKey(sender, "messages.fly.disabled_other", target.getName());
                        Notification.infoKey(target, "messages.fly.disabled_self");
                    } else {
                        target.setAllowFlight(true);
                        Notification.infoKey(sender, "messages.fly.enabled_other", target.getName());
                        Notification.infoKey(target, "messages.fly.enabled_self");
                    }

                }
            });
            return true;
        }
    }
}
