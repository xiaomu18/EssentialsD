package et.xiaomu.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class God implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player) && args.length < 1) {
            Notification.errorKey(sender, "messages.god.console_requires_player");
            return false;
        } else {
            Player target = Apis.getPlayerFromArg(sender, args, 0);
            if (target == null) {
                return true;
            }
            Scheduler.runEntityTask(target, () -> {
                if (!target.isOnline()) {
                    Notification.errorKey(sender, "messages.api.player_not_online", target.getName());
                    return;
                }
                if (target.isInvulnerable()) {
                    if (et.xiaomu.essentialsd.EssentialsD.vanishManager.isVanished(target)) {
                        Notification.errorKey(sender, "messages.god.disable_blocked_by_vanish_sender", target.getName());
                        if (!sender.equals(target)) {
                            Notification.warnKey(target, "messages.god.disable_blocked_by_vanish_target", sender.getName());
                        }
                        return;
                    }
                    target.setInvulnerable(false);
                    Notification.infoKey(sender, "messages.god.disabled_other", target.getName());
                    Notification.infoKey(target, "messages.god.disabled_self");
                } else {
                    target.setInvulnerable(true);
                    Notification.infoKey(sender, "messages.god.enabled_other", target.getName());
                    Notification.infoKey(target, "messages.god.enabled_self");
                }
            });
            return true;
        }
    }
}
