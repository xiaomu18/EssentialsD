package et.xiaomu.essentialsd.commands.warp;

import et.xiaomu.essentialsd.dtos.WarpPoint;
import cn.lunadeer.utils.Notification;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DelWarp implements TabExecutor {
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length != 1) {
            Notification.errorKey(commandSender, "messages.delwarp.usage");
            return true;
        } else {
            WarpPoint point = WarpPoint.selectByName(strings[0]);
            if (point == null) {
                Notification.errorKey(commandSender, "messages.warp.not_found", strings[0]);
                return true;
            } else {
                WarpPoint.delete(point);
                Notification.infoKey(commandSender, "messages.delwarp.deleted", strings[0]);
                return true;
            }
        }
    }

    public @Nullable List onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return WarpPoint.selectAllNames();
    }
}
