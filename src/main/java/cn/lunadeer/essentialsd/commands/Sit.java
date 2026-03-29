package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.utils.SeatManager;
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
            Notification.error(sender, "Only players can use this command.");
            return true;
        }
        if (!EssentialsD.config.getChairEnable()) {
            Notification.warn(player, "Sit is currently disabled.");
            return true;
        }
        if (!player.isOnGround()) {
            Notification.warn(player, "You must stand on the ground before sitting.");
            return true;
        }
        if (player.getLocation().getBlock().isLiquid() || player.getLocation().subtract(0.0, 0.1, 0.0).getBlock().isEmpty()) {
            Notification.warn(player, "You cannot sit at this location.");
            return true;
        }

        Scheduler.runEntityTask(player, () -> {
            Location seatLocation = player.getLocation().clone().add(0.0, EssentialsD.config.getChairSitHeight(), 0.0);
            seatLocation.setPitch(0.0F);
            if (!SeatManager.sit(player, seatLocation)) {
                Notification.warn(player, "You cannot sit right now.");
                return;
            }
            Notification.info(player, "You sit down.");
        });
        return true;
    }
}
