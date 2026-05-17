package et.xiaomu.essentialsd.commands.warp;

import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.dtos.WarpPoint;
import cn.lunadeer.utils.Notification;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Warp implements TabExecutor {
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player)) {
            Notification.warnKey(commandSender, "messages.common.player_only_command");
            return true;
        } else {
            Player player = (Player) commandSender;
            if (strings.length != 1) {
                Notification.errorKey(commandSender, "messages.warp.usage");
                return true;
            } else {
                WarpPoint point = WarpPoint.selectByName(strings[0]);
                if (point == null) {
                    Notification.errorKey(commandSender, "messages.warp.not_found", strings[0]);
                    return true;
                } else {
                    try {
                        EssentialsD.tpManager.doTeleportDelayed(player, point.getLocation(), EssentialsD.config.getTpDelay(),
                                () -> Notification.infoKey(player, "messages.warp.teleporting", strings[0]),
                                () -> Notification.infoKey(player, "messages.warp.teleported", strings[0]),
                                EssentialsD.tpManager.createLogContext("warp:" + strings[0]));
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
