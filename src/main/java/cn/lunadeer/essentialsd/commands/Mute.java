package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.minecraftpluginutils.Notification;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Mute implements TabExecutor {
    public Set<UUID> mutedList = new HashSet<>();
    public Set<String> mutedIpList = new HashSet<>();

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equals("mute")) {
            if (args.length == 0) {
                Notification.info(sender, "用法: \n/mute ls 显示已禁言列表\n/mute [player] 禁言玩家\n/mute ip [player/ip] 禁言 ip 地址");
                return true;
            } else if (args[0].equals("ip")) {
                Player player = Bukkit.getPlayer(args[1]);

                if (player != null) {
                    mutedIpList.add(player.getAddress().getHostString());
                    Notification.warn(sender, "已禁言 IP " + player.getAddress().getHostString());
                } else {
                    mutedIpList.add(args[1]);
                    Notification.warn(sender, "已禁言 IP " + args[1]);
                }
                return true;
            } else if (args[0].equals("ls")) {
                String text1 = "";

                for (UUID uuid : mutedList) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                    text1 += player.getName() + ",";
                }

                Notification.info(sender, "已禁言的玩家 (" + mutedList.size() + "): " + text1);
                Notification.info(sender, "已禁言的 ip (" + mutedIpList.size() + "): " + String.join(",", mutedIpList));
                return true;
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);

            mutedList.add(player.getUniqueId());
            Notification.warn(sender, "已禁言玩家 " + player.getName());
        } else if (command.getName().equals("unmute")) {
            if (args.length == 0) {
                Notification.info(sender, "用法: \n/unmute [player] 取消禁言玩家\n/unmute ip [player/ip] 取消禁言 ip 地址");
                return true;
            } else if (args[0].equals("ip")) {
                Player player = Bukkit.getPlayer(args[1]);

                if (player != null) {
                    mutedIpList.remove(player.getAddress().getHostString());
                    Notification.info(sender, "成功取消禁言");
                } else {
                    mutedIpList.remove(args[1]);
                    Notification.info(sender, "成功取消禁言");
                }
                return true;
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);

            mutedList.remove(player.getUniqueId());
            Notification.info(sender, "取消禁言 " + player.getName());
        }

        return true;
    }

    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> words = new ArrayList<>();

        if (command.getName().equals("unmute")) {
            if (args.length == 1) {
                for (UUID uuid : mutedList) {
                    words.add(Bukkit.getOfflinePlayer(uuid).getName());
                }
            } else if (args[0].equals("ip")) {
                words.addAll(mutedIpList);
            }
        } else {
            return null;
        }

        return words;
    }
}