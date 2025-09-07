package cn.lunadeer.essentialsd.commands.home;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.dtos.HomeInfo;
import cn.lunadeer.minecraftpluginutils.Notification;
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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 3) {
            if (!args[1].equals("view")) {
                Notification.warn(sender, "错误的参数");
                return true;
            }
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
        List<HomeInfo> homes = HomeInfo.getHomesOf(player.getUniqueId());

        if (args[1].equals("tp")) {
            if (!(sender instanceof Player)) {
                Notification.warn(sender, "仅玩家可使用此命令");
                return true;
            }

            for (HomeInfo home : homes) {
                if (home.homeName.equals(args[2])) {
                    EssentialsD.tpManager.doTeleportDelayed((Player) sender, home.location, 0, () -> Notification.info(sender, "正在传送..."), () -> Notification.info(sender, "成功传送到 " + player.getName() + " 的家 " + home.homeName));
                    return true;
                }
            }

            Notification.warn(sender, "玩家 " + player.getName() + " 没有名为 " + args[2] + " 的家");
            return true;

        } else if (args[1].equals("view")) {
            int n = 0;

            Notification.info(sender, "玩家 " + player.getName() + " 共有 " + homes.size() + " 个家：");

            for (HomeInfo home : homes) {
                n += 1;
                Notification.info(sender, "[" + n + "] " + home.homeName + " | 位置: " + home.location.getWorld().getName() + " " + (int) home.location.getX() + ", " + (int) home.location.getY() + ", " + (int) home.location.getZ());
            }

            return true;
        } else if (args[1].equals("remove")) {

            for (HomeInfo home : homes) {
                if (home.homeName.equals(args[2])) {
                    boolean res = HomeInfo.deleteHome(player.getUniqueId(), home.homeName);
                    if (res) {
                        Notification.info(sender, "成功删除玩家 "+ player.getName() + " 的家 " + home.homeName);
                    } else {
                        Notification.error(sender, "删除失败");
                    }
                    return true;
                }
            }

            Notification.warn(sender, "玩家 " + player.getName() + " 没有名为 " + args[2] + " 的家");
            return true;
        }

        Notification.warn(sender, "未知用法");
        return true;
    }

    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> words = new ArrayList<>();

        if (args.length == 1) {
            return null;
        } else if (args.length == 2) {
            words.add("tp");
            words.add("view");
            words.add("remove");
            return words;
        } else if (args.length == 3 && !args[1].equals("view")) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
            List<HomeInfo> homes = HomeInfo.getHomesOf(player.getUniqueId());
            for (HomeInfo home : homes) {
                if (home.homeName.startsWith(args[2])) {
                    words.add(home.homeName);
                }
            }
            return words;
        }

        return words;
    }
}
