package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.minecraftpluginutils.Notification;
import cn.lunadeer.minecraftpluginutils.Scheduler;
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

public class Invisible implements CommandExecutor {
    public List<UUID> invList = new ArrayList<>();
    public BossBar invBossBar = Bukkit.createBossBar("§7你正处于隐身状态", BarColor.WHITE, BarStyle.SEGMENTED_6);

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player;

        if (args.length == 0) {
            if (sender instanceof Player) {
                Notification.info(sender, "你的隐身模式状态: " + invList.contains(((Player) sender).getUniqueId()));
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
                Notification.info(sender, "已隐身的玩家: " + String.join(", ", invNameList));
                return true;
            }
            if (sender instanceof Player) {
                if (args[0].equals("on")) {
                    enable_player_inv((Player) sender);
                    Notification.info(sender, "你的隐身模式 已启用");
                } else if (args[0].equals("off")) {
                    disable_player_inv((Player) sender);
                    Notification.info(sender, "你的隐身模式 已禁用");
                }
            } else {
                Notification.info(sender, "用法: /inv [player] on/off");
            }
            return true;
        }

        if (args.length == 2) {
            player = Bukkit.getPlayer(args[0]);

            if (player == null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);

                if (args[1].equals("on")) {
                    if (!invList.contains(offlinePlayer.getUniqueId())) {
                        invList.add(offlinePlayer.getUniqueId());
                        Notification.info(sender, offlinePlayer.getName() + " 的隐身模式 已启用");
                    }
                } else if (args[1].equals("off")) {
                    if (invList.contains(offlinePlayer.getUniqueId())) {
                        invList.remove(offlinePlayer.getUniqueId());
                        Notification.info(sender, offlinePlayer.getName() + " 的隐身模式 已禁用");
                    }
                }
                return true;
            }

            if (args[1].equals("on")) {
                enable_player_inv(player);
                Notification.info(sender, player.getName() + " 的隐身模式 已启用");
            } else if (args[1].equals("off")) {
                disable_player_inv(player);
                Notification.info(sender, player.getName() + " 的隐身模式 已禁用");
            }
        }
        return true;
    }

    private void enable_player_inv(Player player) {
        if (invList.contains(player.getUniqueId())) {
            return;
        }

        invList.add(player.getUniqueId());

        Scheduler.runTask(() -> {
            for (Player player1 : Bukkit.getOnlinePlayers()) {
                if (player1.equals(player) || player1.isOp()) {
                    continue;
                }
                player1.hidePlayer(EssentialsD.instance, player);
            }
            invBossBar.addPlayer(player);
        });
    }

    private void disable_player_inv(Player player) {
        if (invList.contains(player.getUniqueId())) {
            invList.remove(player.getUniqueId());

            Scheduler.runTask(() -> {
                for (Player player1 : Bukkit.getOnlinePlayers()) {
                    if (player1.equals(player)) {
                        continue;
                    }
                    player1.showPlayer(EssentialsD.instance, player);
                }
                invBossBar.removePlayer(player);
            });
        }
    }
}
