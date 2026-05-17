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

import java.util.Collections;
import java.util.List;

public class SetHome implements TabExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            Notification.warnKey(sender, "messages.common.player_only_command");
            return true;
        } else {
            Player player = (Player) sender;
            if (EssentialsD.config.getHomeWorldBlacklist().contains(player.getWorld().getName())) {
                Notification.errorKey(player, "messages.sethome.world_blocked", player.getWorld().getName());
                return true;
            }
            List<HomeInfo> homes = HomeInfo.getHomesOf(((Player) sender).getUniqueId());
            if (homes.size() > EssentialsD.config.getHomeLimitAmount()) {
                Notification.errorKey(player, "messages.sethome.limit_reached");
                return true;
            } else {
                HomeInfo info = new HomeInfo();
                info.uuid = player.getUniqueId();
                if (args.length == 0) {
                    info.homeName = "default";
                } else {
                    info.homeName = args[0];
                }

                info.location = player.getLocation();
                HomeInfo exist = HomeInfo.getHome(player.getUniqueId(), info.homeName);
                if (exist != null) {
                    Notification.errorKey(player, "messages.sethome.already_exists", info.homeName);
                    return true;
                } else {
                    boolean res = HomeInfo.newHome(info);
                    if (res) {
                        Notification.infoKey(player, "messages.sethome.created", info.homeName);
                    } else {
                        Notification.errorKey(player, "messages.sethome.create_failed", info.homeName);
                    }

                    return true;
                }
            }
        }
    }

    public @Nullable List onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? Collections.singletonList("home-name") : null;
    }
}
