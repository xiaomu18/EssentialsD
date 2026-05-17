package et.xiaomu.essentialsd.commands.home;

import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.dtos.HomeInfo;
import cn.lunadeer.utils.Notification;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HomeEditor implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            Notification.warnKey(sender, "messages.home_editor.invalid_args");
            return true;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
        List<HomeInfo> homes = HomeInfo.getHomesOf(player.getUniqueId());

        if (args[1].equals("tp")) {
            if (args.length < 3) {
                Notification.warnKey(sender, "messages.home_editor.invalid_args");
                return true;
            }
            if (!(sender instanceof Player commandPlayer)) {
                Notification.warnKey(sender, "messages.home_editor.player_only_tp");
                return true;
            }
            for (HomeInfo home : homes) {
                if (home.homeName.equals(args[2])) {
                    if (EssentialsD.config.getHomeWorldBlacklist().contains(home.location.getWorld().getName())) {
                        Notification.warnKey(sender, "messages.home_editor.world_blocked", player.getName(), home.homeName, home.location.getWorld().getName());
                        return true;
                    }
                    EssentialsD.tpManager.doTeleportDelayed(commandPlayer, home.location, 0,
                            () -> Notification.infoKey(sender, "messages.home_editor.teleporting"),
                            () -> Notification.infoKey(sender, "messages.home_editor.teleported", player.getName(), home.homeName),
                            EssentialsD.tpManager.createLogContext("home-editor:" + player.getName() + "/" + home.homeName));
                    return true;
                }
            }
            Notification.warnKey(sender, "messages.home_editor.not_found", player.getName(), args[2]);
            return true;
        }

        if (args[1].equals("view")) {
            Notification.infoKey(sender, "messages.home_editor.view_header", player.getName(), homes.size());
            int n = 0;
            for (HomeInfo home : homes) {
                n++;
                Notification.infoKey(sender, "messages.home_editor.view_entry", n, home.homeName, home.location.getWorld().getName(),
                        (int) home.location.getX(), (int) home.location.getY(), (int) home.location.getZ());
            }
            return true;
        }

        if (args[1].equals("remove")) {
            if (args.length < 3) {
                Notification.warnKey(sender, "messages.home_editor.invalid_args");
                return true;
            }
            for (HomeInfo home : homes) {
                if (home.homeName.equals(args[2])) {
                    if (HomeInfo.deleteHome(player.getUniqueId(), home.homeName)) {
                        Notification.infoKey(sender, "messages.home_editor.deleted", player.getName(), home.homeName);
                    } else {
                        Notification.errorKey(sender, "messages.home_editor.delete_failed");
                    }
                    return true;
                }
            }
            Notification.warnKey(sender, "messages.home_editor.not_found", player.getName(), args[2]);
            return true;
        }

        Notification.warnKey(sender, "messages.home_editor.unknown_usage");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> words = new ArrayList<>();
        if (args.length == 1) {
            return null;
        }
        if (args.length == 2) {
            words.add("tp");
            words.add("view");
            words.add("remove");
            return words;
        }
        if (args.length == 3 && !args[1].equals("view")) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
            List<HomeInfo> homes = HomeInfo.getHomesOf(player.getUniqueId());
            for (HomeInfo home : homes) {
                if (home.homeName.startsWith(args[2])) {
                    words.add(home.homeName);
                }
            }
        }
        return words;
    }
}
