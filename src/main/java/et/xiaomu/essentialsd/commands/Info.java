package et.xiaomu.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import et.xiaomu.essentialsd.managers.inspect.OfflinePlayerDataAccess;
import et.xiaomu.essentialsd.utils.MuteDuration;
import et.xiaomu.essentialsd.utils.PlayerLookup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static cn.lunadeer.utils.stui.ViewStyles.ACTION;
import static cn.lunadeer.utils.stui.ViewStyles.NORMAL;
import static cn.lunadeer.utils.stui.ViewStyles.PRIMARY;
import static cn.lunadeer.utils.stui.ViewStyles.SECONDARY;
import static cn.lunadeer.utils.stui.ViewStyles.SEVERE;

public class Info implements TabExecutor {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("essd.info")) {
            Notification.error(sender, "你没有权限使用这个命令");
            return true;
        }
        if (args.length != 1) {
            Notification.error(sender, "用法: /info <player>");
            return true;
        }

        OfflinePlayer target = PlayerLookup.resolve(args[0]);
        if (target == null) {
            Notification.error(sender, "找不到玩家: %s", args[0]);
            return true;
        }

        if (target.isOnline()) {
            Player online = target.getPlayer();
            if (online == null) {
                Notification.error(sender, "无法读取玩家 %s 的在线数据", args[0]);
                return true;
            }
            Scheduler.runEntityTask(online, () -> dispatchToSender(sender, () -> sendInfo(sender, fromOnline(online))));
            return true;
        }

        Scheduler.runTaskAsync(() -> {
            OfflinePlayerDataAccess.PlayerDataSnapshot snapshot = OfflinePlayerDataAccess.loadSnapshot(target);
            dispatchToSender(sender, () -> {
                if (snapshot == null) {
                    Notification.error(sender, "无法读取玩家 %s 的离线数据", PlayerLookup.displayName(target));
                    return;
                }
                sendInfo(sender, fromOffline(target, snapshot));
            });
        });
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        List<String> words = new ArrayList<>();
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            words.add(player.getName());
        }
        String lower = args[0].toLowerCase(Locale.ROOT);
        return words.stream()
                .filter(word -> word.toLowerCase(Locale.ROOT).startsWith(lower))
                .distinct()
                .toList();
    }

    private PlayerInfo fromOnline(Player player) {
        long loginTime = player.getLastLogin();
        Long totalPlayTime = readOnlinePlayTime(player);
        return new PlayerInfo(
                PlayerLookup.displayName(player),
                player.getUniqueId(),
                true,
                player.getGameMode().name(),
                player.getWorld().getName(),
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ(),
                player.isInvulnerable(),
                player.getAllowFlight(),
                player.getFlySpeed(),
                player.getWalkSpeed(),
                loginTime > 0L ? loginTime : null,
                loginTime > 0L ? Math.max(0L, System.currentTimeMillis() - loginTime) : null,
                null,
                totalPlayTime
        );
    }

    private PlayerInfo fromOffline(OfflinePlayer player, OfflinePlayerDataAccess.PlayerDataSnapshot snapshot) {
        long lastSeen = snapshot.lastSeenMillis();
        return new PlayerInfo(
                PlayerLookup.displayName(player),
                player.getUniqueId(),
                false,
                snapshot.gameMode().name(),
                snapshot.worldName(),
                snapshot.x(),
                snapshot.y(),
                snapshot.z(),
                snapshot.invulnerable(),
                snapshot.allowFlight(),
                snapshot.flySpeed(),
                snapshot.walkSpeed(),
                null,
                null,
                lastSeen > 0L ? Math.max(0L, System.currentTimeMillis() - lastSeen) : null,
                snapshot.totalPlayTimeMillis()
        );
    }

    private void sendInfo(CommandSender sender, PlayerInfo info) {
        TextColor statusColor = info.online() ? NORMAL : SEVERE;
        String statusText = info.online() ? "在线" : "离线";

        sender.sendMessage(Component.text("┏━━ 玩家信息 ━━ ", PRIMARY)
                .append(Component.text(info.displayName(), ACTION))
                .append(Component.text(" [" + statusText + "]", statusColor)));
        sender.sendMessage(joinPairs(
                pair("UUID", Component.text(info.uuid().toString(), SECONDARY)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard(info.uuid().toString()))
                        .hoverEvent(Component.text("点击复制 UUID", ACTION))),
                Component.text(prettyGameMode(info.gameMode()), ACTION)
        ));
        sender.sendMessage(joinPairs(
                pair("坐标", String.format(Locale.US, "%s (%.2f, %.2f, %.2f)", info.worldName(), info.x(), info.y(), info.z()), NORMAL),
                pair("Invulnerable", info.invulnerable() ? "是" : "否", info.invulnerable() ? NORMAL : SEVERE)
        ));
        sender.sendMessage(joinTriple(
                pair("AllowFlight", info.allowFlight() ? "是" : "否", info.allowFlight() ? NORMAL : SEVERE),
                pair("飞行速度", String.format(Locale.US, "%.3f", info.flySpeed()), ACTION),
                pair("移动速度", String.format(Locale.US, "%.3f", info.walkSpeed()), ACTION)
        ));
        sender.sendMessage(joinSingle(
                pair(info.online() ? "上线时间" : "上次在线", formatTimestamp(info.online() ? info.loginAtMillis() : resolveLastOnlineMillis(info)), NORMAL)
        ));
        sender.sendMessage(joinPairs(
                pair(info.online() ? "已在线多久" : "已离线多久", formatDuration(info.online() ? info.onlineDurationMillis() : info.offlineDurationMillis()), ACTION),
                pair("总游玩时长", formatDuration(info.totalPlayTimeMillis()), NORMAL)
        ));
        sender.sendMessage(Component.text("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━", PRIMARY));
    }

    private @Nullable Long resolveLastOnlineMillis(PlayerInfo info) {
        return info.offlineDurationMillis() == null ? null : System.currentTimeMillis() - info.offlineDurationMillis();
    }

    private @Nullable Long readOnlinePlayTime(Player player) {
        try {
            return player.getStatistic(Statistic.PLAY_ONE_MINUTE) * 50L;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void dispatchToSender(CommandSender sender, Runnable action) {
        if (sender instanceof Player player) {
            Scheduler.runEntityTask(player, action);
            return;
        }
        Scheduler.runTask(action);
    }

    private Component joinPairs(Component first, Component second) {
        return Component.text()
                .append(Component.text("┃ ", PRIMARY))
                .append(first)
                .append(Component.text(" | ", SECONDARY))
                .append(second)
                .build();
    }

    private Component joinTriple(Component first, Component second, Component third) {
        return Component.text()
                .append(Component.text("┃ ", PRIMARY))
                .append(first)
                .append(Component.text(" | ", SECONDARY))
                .append(second)
                .append(Component.text(" | ", SECONDARY))
                .append(third)
                .build();
    }

    private Component joinSingle(Component content) {
        return Component.text()
                .append(Component.text("┃ ", PRIMARY))
                .append(content)
                .build();
    }

    private Component pair(String label, String value, TextColor valueColor) {
        return pair(label, Component.text(value, valueColor));
    }

    private Component pair(String label, Component value) {
        return Component.text(label + ": ", SECONDARY).append(value);
    }

    private String prettyGameMode(String mode) {
        return switch (mode.toUpperCase(Locale.ROOT)) {
            case "CREATIVE" -> "创造";
            case "ADVENTURE" -> "冒险";
            case "SPECTATOR" -> "旁观";
            default -> "生存";
        };
    }

    private String formatTimestamp(@Nullable Long millis) {
        if (millis == null || millis <= 0L) {
            return "未知";
        }
        return TIME_FORMATTER.format(Instant.ofEpochMilli(millis));
    }

    private String formatDuration(@Nullable Long millis) {
        if (millis == null) {
            return "未知";
        }
        return MuteDuration.formatDuration(Math.max(0L, millis));
    }

    private record PlayerInfo(
            String displayName,
            UUID uuid,
            boolean online,
            String gameMode,
            String worldName,
            double x,
            double y,
            double z,
            boolean invulnerable,
            boolean allowFlight,
            float flySpeed,
            float walkSpeed,
            @Nullable Long loginAtMillis,
            @Nullable Long onlineDurationMillis,
            @Nullable Long offlineDurationMillis,
            @Nullable Long totalPlayTimeMillis
    ) {
    }
}
