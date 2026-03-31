package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.managers.VanishManager;
import cn.lunadeer.utils.Notification;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class Vanish implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            if (sender instanceof Player player) {
                sendStatus(sender, player);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subCommand) {
            case "list" -> handleList(sender, args);
            case "on" -> handleSet(sender, args, true);
            case "off" -> handleSet(sender, args, false);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Stream.of("list", "on", "off")
                    .filter(subCommand -> subCommand.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2
                && sender.hasPermission(VanishManager.PERMISSION_OTHER)
                && ("on".equalsIgnoreCase(args[0]) || "off".equalsIgnoreCase(args[0]))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        return List.of();
    }

    private boolean handleList(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sendUsage(sender);
            return true;
        }
        if (!EssentialsD.vanishManager.canSeeVanished(sender)) {
            Notification.error(sender, "你没有权限查看隐身玩家列表");
            return true;
        }
        List<Player> vanishedPlayers = EssentialsD.vanishManager.getVanishedPlayers();
        if (vanishedPlayers.isEmpty()) {
            Notification.info(sender, "当前没有隐身中的在线玩家");
            return true;
        }

        Notification.info(sender, Component.text("隐身玩家列表 ", NamedTextColor.GRAY)
                .append(Component.text("(" + vanishedPlayers.size() + ")", NamedTextColor.YELLOW)));
        for (Player player : vanishedPlayers) {
            Notification.info(sender, formatPlayerLine(player));
        }
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args, boolean enable) {
        Player target;
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                Notification.error(sender, "控制台必须指定目标玩家");
                return true;
            }
            target = player;
        } else if (args.length == 2) {
            if (!sender.hasPermission(VanishManager.PERMISSION_OTHER)) {
                Notification.error(sender, "你没有权限修改其他玩家的隐身状态");
                return true;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                Notification.error(sender, "玩家 %s 不在线", args[1]);
                return true;
            }
        } else {
            sendUsage(sender);
            return true;
        }

        if (!enable && EssentialsD.vanishManager.isForced(target.getUniqueId())) {
            Notification.error(sender, "%s 当前处于强制隐身状态，无法关闭", target.getName());
            if (!sender.equals(target)) {
                Notification.warn(target, "%s 尝试关闭你的隐身状态，但你当前处于强制隐身状态", sender.getName());
            }
            return true;
        }

        boolean changed = EssentialsD.vanishManager.setManualVanished(target, enable);
        if (!changed) {
            if (enable) {
                Notification.warn(sender, "%s 已经处于隐身状态", sender.equals(target) ? "你" : target.getName());
            } else {
                Notification.warn(sender, "%s 当前未处于可关闭的隐身状态", sender.equals(target) ? "你" : target.getName());
            }
            return true;
        }

        if (sender.equals(target)) {
            if (enable) {
                Notification.info(target, EssentialsD.vanishManager.isForced(target.getUniqueId())
                        ? "已开启隐身模式，且你当前同时受到强制隐身限制"
                        : "已开启隐身模式");
            } else {
                Notification.info(target, "已关闭隐身模式");
            }
            return true;
        }

        Notification.info(sender, "已将 %s 的隐身状态设置为 %s", target.getName(), enable ? "开启" : "关闭");
        Notification.info(target, "%s 已将你的隐身状态设置为 %s", sender.getName(), enable ? "开启" : "关闭");
        return true;
    }

    private void sendUsage(CommandSender sender) {
        Notification.info(sender, "用法: /vanish <list|on|off> [玩家]");
    }

    private void sendStatus(CommandSender sender, Player player) {
        boolean vanished = EssentialsD.vanishManager.isVanished(player);
        boolean forced = EssentialsD.vanishManager.isForced(player.getUniqueId());
        Component status = Component.text("当前状态: ", NamedTextColor.GRAY)
                .append(Component.text(vanished ? "已隐身" : "未隐身", vanished ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(forced ? Component.text(" [强制]", NamedTextColor.GOLD) : Component.empty());
        Notification.info(sender, status);
    }

    private Component formatPlayerLine(Player player) {
        Component line = Component.text(" - ", NamedTextColor.DARK_GRAY)
                .append(Component.text(player.getName(), NamedTextColor.YELLOW));
        if (EssentialsD.vanishManager.isForced(player.getUniqueId())) {
            line = line.append(Component.text(" [强制]", NamedTextColor.GOLD));
        }
        if (EssentialsD.vanishManager.canChatWhileVanished(player)) {
            line = line.append(Component.text(" [可聊天]", NamedTextColor.AQUA));
        }
        return line;
    }
}
