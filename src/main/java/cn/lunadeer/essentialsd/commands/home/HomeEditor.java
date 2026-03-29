package cn.lunadeer.essentialsd.commands.home;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.dtos.HomeInfo;
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
            Notification.warn(sender, "错误的参数");
            return true;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
        List<HomeInfo> homes = HomeInfo.getHomesOf(player.getUniqueId());

        if (args[1].equals("tp")) {
            if (args.length < 3) {
                Notification.warn(sender, "错误的参数");
                return true;
            }
            if (!(sender instanceof Player commandPlayer)) {
                Notification.warn(sender, "仅玩家可使用此命令");
                return true;
            }
            for (HomeInfo home : homes) {
                if (home.homeName.equals(args[2])) {
                    EssentialsD.tpManager.doTeleportDelayed(commandPlayer, home.location, 0,
                            () -> Notification.info(sender, "正在传送..."),
                            () -> Notification.info(sender, "成功传送到 %s 的家 %s", player.getName(), home.homeName));
                    return true;
                }
            }
            Notification.warn(sender, "玩家 %s 没有名为 %s 的家", player.getName(), args[2]);
            return true;
        }

        if (args[1].equals("view")) {
            Notification.info(sender, "玩家 %s 共有 %d 个家：", player.getName(), homes.size());
            int n = 0;
            for (HomeInfo home : homes) {
                n++;
                Notification.info(sender, "[%d] %s | 位置: %s %d, %d, %d", n, home.homeName, home.location.getWorld().getName(),
                        (int) home.location.getX(), (int) home.location.getY(), (int) home.location.getZ());
            }
            return true;
        }

        if (args[1].equals("remove")) {
            if (args.length < 3) {
                Notification.warn(sender, "错误的参数");
                return true;
            }
            for (HomeInfo home : homes) {
                if (home.homeName.equals(args[2])) {
                    if (HomeInfo.deleteHome(player.getUniqueId(), home.homeName)) {
                        Notification.info(sender, "成功删除玩家 %s 的家 %s", player.getName(), home.homeName);
                    } else {
                        Notification.error(sender, "删除失败");
                    }
                    return true;
                }
            }
            Notification.warn(sender, "玩家 %s 没有名为 %s 的家", player.getName(), args[2]);
            return true;
        }

        Notification.warn(sender, "未知用法");
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
