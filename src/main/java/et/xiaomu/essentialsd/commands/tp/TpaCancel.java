package et.xiaomu.essentialsd.commands.tp;

import et.xiaomu.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpaCancel implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Notification.warnKey(sender, "messages.common.player_only_command");
            return true;
        }

        if (args.length != 0) {
            Notification.errorKey(player, "messages.tpa_cancel.invalid_args");
            return false;
        }

        EssentialsD.tpManager.cancelRequests(player);
        return true;
    }
}
