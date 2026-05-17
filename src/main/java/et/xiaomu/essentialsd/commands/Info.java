package et.xiaomu.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import et.xiaomu.essentialsd.EssentialsD;
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
            Notification.errorKey(sender, "messages.info.no_permission");
            return true;
        }
        if (args.length != 1) {
            Notification.errorKey(sender, "messages.info.usage");
            return true;
        }

        OfflinePlayer target = PlayerLookup.resolve(args[0]);
        if (target == null) {
            Notification.errorKey(sender, "messages.info.player_not_found", args[0]);
            return true;
        }

        if (target.isOnline()) {
            Player online = target.getPlayer();
            if (online == null) {
                Notification.errorKey(sender, "messages.info.read_online_failed", args[0]);
                return true;
            }
            Scheduler.runEntityTask(online, () -> dispatchToSender(sender, () -> sendInfo(sender, fromOnline(online))));
            return true;
        }

        Scheduler.runTaskAsync(() -> {
            OfflinePlayerDataAccess.PlayerDataSnapshot snapshot = OfflinePlayerDataAccess.loadSnapshot(target);
            dispatchToSender(sender, () -> {
                if (snapshot == null) {
                    Notification.errorKey(sender, "messages.info.read_offline_failed", PlayerLookup.displayName(target));
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
        String statusText = info.online() ? EssentialsD.localization.get("common.online") : EssentialsD.localization.get("common.offline");

        sender.sendMessage(Component.text(EssentialsD.localization.get("ui.info.header"), PRIMARY)
                .append(Component.text(info.displayName(), ACTION))
                .append(Component.text(" [" + statusText + "]", statusColor)));
        sender.sendMessage(joinPairs(
                pair(EssentialsD.localization.get("ui.info.uuid_label"), Component.text(info.uuid().toString(), SECONDARY)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard(info.uuid().toString()))
                        .hoverEvent(Component.text(EssentialsD.localization.get("ui.info.copy_uuid_hover"), ACTION))),
                Component.text(prettyGameMode(info.gameMode()), ACTION)
        ));
        sender.sendMessage(joinPairs(
                pair(EssentialsD.localization.get("ui.info.location_label"), String.format(Locale.US, "%s (%.2f, %.2f, %.2f)", info.worldName(), info.x(), info.y(), info.z()), NORMAL),
                pair(EssentialsD.localization.get("ui.info.invulnerable_label"), info.invulnerable() ? EssentialsD.localization.get("common.yes") : EssentialsD.localization.get("common.no"), info.invulnerable() ? NORMAL : SEVERE)
        ));
        sender.sendMessage(joinTriple(
                pair(EssentialsD.localization.get("ui.info.allow_flight_label"), info.allowFlight() ? EssentialsD.localization.get("common.yes") : EssentialsD.localization.get("common.no"), info.allowFlight() ? NORMAL : SEVERE),
                pair(EssentialsD.localization.get("ui.info.fly_speed_label"), String.format(Locale.US, "%.3f", info.flySpeed()), ACTION),
                pair(EssentialsD.localization.get("ui.info.walk_speed_label"), String.format(Locale.US, "%.3f", info.walkSpeed()), ACTION)
        ));
        sender.sendMessage(joinSingle(
                pair(info.online() ? EssentialsD.localization.get("ui.info.login_time_label") : EssentialsD.localization.get("ui.info.last_seen_label"),
                        formatTimestamp(info.online() ? info.loginAtMillis() : resolveLastOnlineMillis(info)), NORMAL)
        ));
        sender.sendMessage(joinPairs(
                pair(info.online() ? EssentialsD.localization.get("ui.info.online_duration_label") : EssentialsD.localization.get("ui.info.offline_duration_label"),
                        formatDuration(info.online() ? info.onlineDurationMillis() : info.offlineDurationMillis()), ACTION),
                pair(EssentialsD.localization.get("ui.info.total_play_time_label"), formatDuration(info.totalPlayTimeMillis()), NORMAL)
        ));
        sender.sendMessage(Component.text(EssentialsD.localization.get("ui.info.footer"), PRIMARY));
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
            case "CREATIVE" -> EssentialsD.localization.get("common.gamemode.creative");
            case "ADVENTURE" -> EssentialsD.localization.get("common.gamemode.adventure");
            case "SPECTATOR" -> EssentialsD.localization.get("common.gamemode.spectator");
            default -> EssentialsD.localization.get("common.gamemode.survival");
        };
    }

    private String formatTimestamp(@Nullable Long millis) {
        if (millis == null || millis <= 0L) {
            return EssentialsD.localization.get("common.unknown");
        }
        return TIME_FORMATTER.format(Instant.ofEpochMilli(millis));
    }

    private String formatDuration(@Nullable Long millis) {
        if (millis == null) {
            return EssentialsD.localization.get("common.unknown");
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
