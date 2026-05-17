package et.xiaomu.essentialsd.commands.tp;

import et.xiaomu.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpaHere implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            Notification.warnKey(sender, "messages.common.player_only_command");
            return true;
        } else {
            Player player = (Player) sender;
            if (args.length == 1) {
                if (EssentialsD.vanishManager.isVanished(player) && !EssentialsD.vanishManager.canTpahereWhileVanished(player)) {
                    Notification.warnKey(player, "messages.tpahere.blocked_by_vanish");
                    return true;
                }
                Player target = EssentialsD.instance.getServer().getPlayer(args[0]);
                if (target == null || EssentialsD.vanishManager.isHiddenFrom(player, target)) {
                    Notification.warnKey(player, "messages.api.player_not_online", args[0]);
                    return true;
                } else {
                    EssentialsD.tpManager.tpahereRequest(player, target);
                    return true;
                }
            } else {
                Notification.errorKey(player, "messages.tpahere.invalid_args");
                return false;
            }
        }
    }
}
