package et.xiaomu.essentialsd.commands;

import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.utils.SeatManager;
import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Sit implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Notification.error(sender, "只有玩家可以使用此命令。");
            return true;
        }
        if (!EssentialsD.config.getChairEnable()) {
            Notification.warn(player, "坐下功能当前已禁用。");
            return true;
        }
        if (!player.isOnGround()) {
            Notification.warn(player, "你必须站在地面上才能坐下。");
            return true;
        }
        if (player.getLocation().getBlock().isLiquid() || player.getLocation().subtract(0.0, 0.1, 0.0).getBlock().isEmpty()) {
            Notification.warn(player, "你无法在此处坐下。");
            return true;
        }

        Scheduler.runEntityTask(player, () -> {
            Location seatLocation = player.getLocation().clone().add(0.0, EssentialsD.config.getChairSitHeight(), 0.0);
            seatLocation.setPitch(0.0F);
            if (!SeatManager.sit(player, seatLocation)) {
                Notification.warn(player, "你现在无法坐下。");
                return;
            }
            Notification.info(player, "你坐下了。");
        });
        return true;
    }
}
