package cn.lunadeer.essentialsd.commands.tp;

import cn.lunadeer.essentialsd.EssentialsD;
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
            Notification.warn(sender, "只有玩家可以使用此命令");
            return true;
        }

        if (args.length != 0) {
            Notification.error(player, "参数错误");
            return false;
        }

        EssentialsD.tpManager.cancelRequests(player);
        return true;
    }
}
