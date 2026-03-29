package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Vanish implements CommandExecutor {
    public List<UUID> invList = new ArrayList<>();
    public BossBar invBossBar = Bukkit.createBossBar("§7你正处于隐身状态", BarColor.WHITE, BarStyle.SEGMENTED_6);

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                Notification.info(sender, "你的隐身模式状态: %s", invList.contains(player.getUniqueId()));
            } else {
                Notification.info(sender, "用法: /inv [player] on/off");
            }
            return true;
        }

        if (args.length == 1) {
            if (args[0].equals("ls")) {
                if (invList.isEmpty()) {
                    Notification.info(sender, "没有隐身的玩家");
                    return true;
                }
                List<String> invNameList = new ArrayList<>();
                for (UUID uuid : invList) {
                    invNameList.add(Bukkit.getOfflinePlayer(uuid).getName());
                }
                Notification.info(sender, "已隐身的玩家: %s", String.join(", ", invNameList));
                return true;
            }
            if (sender instanceof Player player) {
                if (args[0].equals("on")) {
                    enablePlayerInv(player);
                    Notification.info(sender, "你的隐身模式 已启用");
                } else if (args[0].equals("off")) {
                    disablePlayerInv(player);
                    Notification.info(sender, "你的隐身模式 已禁用");
                }
            } else {
                Notification.info(sender, "用法: /inv [player] on/off");
            }
            return true;
        }

        if (args.length == 2) {
            Player player = Bukkit.getPlayer(args[0]);
            if (player == null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
                if (args[1].equals("on")) {
                    if (!invList.contains(offlinePlayer.getUniqueId())) {
                        invList.add(offlinePlayer.getUniqueId());
                        Notification.info(sender, "%s 的隐身模式 已启用", offlinePlayer.getName());
                    }
                } else if (args[1].equals("off")) {
                    if (invList.contains(offlinePlayer.getUniqueId())) {
                        invList.remove(offlinePlayer.getUniqueId());
                        Notification.info(sender, "%s 的隐身模式 已禁用", offlinePlayer.getName());
                    }
                }
                return true;
            }

            if (!sender.isOp()) {
                Notification.error(sender, "你没有权限操作玩家 %s 的隐身状态", player.getName());
                return true;
            }

            if (args[1].equals("on")) {
                enablePlayerInv(player);
                Notification.info(sender, "%s 的隐身模式 已启用", player.getName());
            } else if (args[1].equals("off")) {
                disablePlayerInv(player);
                Notification.info(sender, "%s 的隐身模式 已禁用", player.getName());
            }
        }
        return true;
    }

    private void enablePlayerInv(Player player) {
        if (invList.contains(player.getUniqueId())) {
            return;
        }
        invList.add(player.getUniqueId());
        Scheduler.runTask(() -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player) || online.isOp()) {
                    continue;
                }
                online.hidePlayer(EssentialsD.instance, player);
            }
            invBossBar.addPlayer(player);
        });
    }

    private void disablePlayerInv(Player player) {
        if (!invList.contains(player.getUniqueId())) {
            return;
        }
        invList.remove(player.getUniqueId());
        Scheduler.runTask(() -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player)) {
                    continue;
                }
                online.showPlayer(EssentialsD.instance, player);
            }
            invBossBar.removePlayer(player);
        });
    }
}
