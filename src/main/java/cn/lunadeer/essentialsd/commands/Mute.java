package cn.lunadeer.essentialsd.commands;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Mute implements TabExecutor {
    public Set<UUID> mutedList = new HashSet<>();
    public Set<String> mutedIpList = new HashSet<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equals("mute")) {
            if (args.length == 0) {
                Notification.info(sender, "用法: \n/mute ls 显示已禁言列表\n/mute [player] 禁言玩家\n/mute ip [player/ip] 禁言 ip 地址");
                return true;
            }
            if (args[0].equals("ip")) {
                Player player = Bukkit.getPlayer(args[1]);
                if (player != null) {
                    mutedIpList.add(player.getAddress().getHostString());
                    Notification.warn(sender, "已禁言 IP %s", player.getAddress().getHostString());
                } else {
                    mutedIpList.add(args[1]);
                    Notification.warn(sender, "已禁言 IP %s", args[1]);
                }
                return true;
            }
            if (args[0].equals("ls")) {
                StringBuilder players = new StringBuilder();
                for (UUID uuid : mutedList) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                    players.append(player.getName()).append(",");
                }
                Notification.info(sender, "已禁言的玩家 (%d): %s", mutedList.size(), players);
                Notification.info(sender, "已禁言的 ip (%d): %s", mutedIpList.size(), String.join(",", mutedIpList));
                return true;
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
            mutedList.add(player.getUniqueId());
            Notification.warn(sender, "已禁言玩家 %s", player.getName());
            return true;
        }

        if (args.length == 0) {
            Notification.info(sender, "用法: \n/unmute [player] 取消禁言玩家\n/unmute ip [player/ip] 取消禁言 ip 地址");
            return true;
        }
        if (args[0].equals("ip")) {
            Player player = Bukkit.getPlayer(args[1]);
            if (player != null) {
                mutedIpList.remove(player.getAddress().getHostString());
            } else {
                mutedIpList.remove(args[1]);
            }
            Notification.info(sender, "成功取消禁言");
            return true;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
        mutedList.remove(player.getUniqueId());
        Notification.info(sender, "取消禁言 %s", player.getName());
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> words = new ArrayList<>();
        if (!command.getName().equals("unmute")) {
            return null;
        }
        if (args.length == 1) {
            for (UUID uuid : mutedList) {
                words.add(Bukkit.getOfflinePlayer(uuid).getName());
            }
        } else if (args[0].equals("ip")) {
            words.addAll(mutedIpList);
        }
        return words;
    }
}
