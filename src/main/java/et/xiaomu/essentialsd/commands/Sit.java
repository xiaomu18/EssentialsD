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
            Notification.errorKey(sender, "messages.common.player_only_command");
            return true;
        }
        if (!EssentialsD.config.getChairEnable()) {
            Notification.warnKey(player, "messages.sit.disabled");
            return true;
        }
        if (!player.isOnGround()) {
            Notification.warnKey(player, "messages.sit.must_be_on_ground");
            return true;
        }
        if (player.getLocation().getBlock().isLiquid() || player.getLocation().subtract(0.0, 0.1, 0.0).getBlock().isEmpty()) {
            Notification.warnKey(player, "messages.sit.invalid_location");
            return true;
        }

        Scheduler.runEntityTask(player, () -> {
            Location seatLocation = player.getLocation().clone().add(0.0, EssentialsD.config.getChairSitHeight(), 0.0);
            seatLocation.setPitch(0.0F);
            if (!SeatManager.sit(player, seatLocation)) {
                Notification.warnKey(player, "messages.sit.cannot_sit");
                return;
            }
            Notification.infoKey(player, "messages.sit.success");
        });
        return true;
    }
}
