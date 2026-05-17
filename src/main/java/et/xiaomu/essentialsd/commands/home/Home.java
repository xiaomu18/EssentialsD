package et.xiaomu.essentialsd.commands.home;

import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.dtos.HomeInfo;
import cn.lunadeer.utils.Notification;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Home implements TabExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            Notification.warnKey(sender, "messages.common.player_only_command");
            return true;
        } else {
            Player player = (Player) sender;
            List<HomeInfo> homes = HomeInfo.getHomesOf(player.getUniqueId());
            String homeName;
            if (args.length == 0) {
                if (homes.isEmpty()) {
                    Notification.errorKey(player, "messages.home.no_home_set");
                    return true;
                }

                homeName = ((HomeInfo) homes.get(0)).homeName;
            } else {
                homeName = args[0];
            }

            HomeInfo home = HomeInfo.getHome(player.getUniqueId(), homeName);
            if (home == null) {
                Notification.errorKey(player, "messages.home.not_found", homeName);
                return true;
            } else if (EssentialsD.config.getHomeWorldBlacklist().contains(home.location.getWorld().getName())) {
                Notification.errorKey(player, "messages.home.world_blocked", homeName, home.location.getWorld().getName());
                return true;
            } else {
                try {
                    EssentialsD.tpManager.doTeleportDelayed(player, home.location, EssentialsD.config.getTpDelay(),
                            () -> Notification.infoKey(player, "messages.home.teleporting", homeName),
                            () -> Notification.infoKey(player, "messages.home.teleported", homeName),
                            EssentialsD.tpManager.createLogContext("home:" + homeName));
                } catch (RuntimeException e) {
                    Notification.errorKey(player, "messages.home.teleport_failed", homeName, e.getMessage());
                }

                return true;
            }
        }
    }

    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<HomeInfo> homes = HomeInfo.getHomesOf(((Player) sender).getUniqueId());
        List<String> res = new ArrayList<>();

        for (HomeInfo home : homes) {
            res.add(home.homeName);
        }

        return res;
    }
}
