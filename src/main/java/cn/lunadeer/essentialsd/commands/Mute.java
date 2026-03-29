package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.dtos.LoginRecord;
import cn.lunadeer.essentialsd.managers.MuteManager;
import cn.lunadeer.essentialsd.utils.MuteDuration;
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
import java.util.Locale;

public class Mute implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("mute")) {
            return handleMute(sender, args);
        }
        return handleUnmute(sender, args);
    }

    private boolean handleMute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showMuteUsage(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("ls")) {
            showMuteList(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("ip")) {
            if (args.length < 2) {
                showMuteUsage(sender);
                return true;
            }

            MuteDuration.ParseResult duration = resolveDuration(sender, args, 2);
            if (duration == null) {
                return true;
            }

            ResolvedIpTarget resolved = resolveIpTarget(args[1]);
            if (resolved == null) {
                Notification.error(sender, "无法解析目标 IP，请提供在线玩家名、最近登录过的玩家名或直接输入 IP");
                return true;
            }

            EssentialsD.muteManager.muteIp(resolved.ip(), resolved.name(), duration.getDurationMillis(), sender.getName());
            Notification.warn(sender, "已禁言 IP %s，持续 %s", resolved.ip(), MuteDuration.formatDuration(duration.getDurationMillis()));
            return true;
        }

        MuteDuration.ParseResult duration = resolveDuration(sender, args, 1);
        if (duration == null) {
            return true;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
        EssentialsD.muteManager.mutePlayer(player, duration.getDurationMillis(), sender.getName());

        String playerName = player.getName();
        if (playerName == null || playerName.isBlank()) {
            playerName = player.getUniqueId().toString();
        }
        Notification.warn(sender, "已禁言玩家 %s，持续 %s", playerName, MuteDuration.formatDuration(duration.getDurationMillis()));
        return true;
    }

    private boolean handleUnmute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showUnmuteUsage(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("ip")) {
            if (args.length < 2) {
                showUnmuteUsage(sender);
                return true;
            }

            ResolvedIpTarget resolved = resolveIpTarget(args[1]);
            if (resolved == null) {
                Notification.error(sender, "无法解析目标 IP");
                return true;
            }

            if (EssentialsD.muteManager.unmuteIp(resolved.ip())) {
                Notification.info(sender, "已取消禁言 IP %s", resolved.ip());
            } else {
                Notification.error(sender, "取消禁言失败");
            }
            return true;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
        if (EssentialsD.muteManager.unmutePlayer(player.getUniqueId())) {
            String playerName = player.getName();
            if (playerName == null || playerName.isBlank()) {
                playerName = player.getUniqueId().toString();
            }
            Notification.info(sender, "已取消禁言 %s", playerName);
        } else {
            Notification.error(sender, "取消禁言失败");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> words = new ArrayList<>();
            if (command.getName().equalsIgnoreCase("mute")) {
                words.add("ls");
                words.add("ip");
                Bukkit.getOnlinePlayers().forEach(player -> words.add(player.getName()));
            } else {
                words.add("ip");
                words.addAll(EssentialsD.muteManager.getMutedPlayerNames());
            }
            return filter(words, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("ip")) {
            List<String> words = new ArrayList<>();
            if (command.getName().equalsIgnoreCase("mute")) {
                Bukkit.getOnlinePlayers().forEach(player -> words.add(player.getName()));
            } else {
                words.addAll(EssentialsD.muteManager.getMutedIps());
            }
            return filter(words, args[1]);
        }

        if (command.getName().equalsIgnoreCase("mute")
                && ((args.length == 2 && !args[0].equalsIgnoreCase("ip")) || (args.length == 3 && args[0].equalsIgnoreCase("ip")))) {
            List<String> words = new ArrayList<>();
            words.add(EssentialsD.config.MUTE_DEFAULT_DURATION);
            words.add("30m");
            words.add("1h");
            words.add("1d");
            words.add("permanent");
            return filter(words, args[args.length - 1]);
        }

        return null;
    }

    private void showMuteUsage(CommandSender sender) {
        Notification.info(sender, "用法:");
        Notification.info(sender, "/mute ls 查看当前禁言列表");
        Notification.info(sender, "/mute <player> [时长] 禁言玩家");
        Notification.info(sender, "/mute ip <player/ip> [时长] 禁言 IP");
        Notification.info(sender, "时长示例: 30m, 1h, 7d, permanent，留空则使用默认时长 %s", EssentialsD.config.MUTE_DEFAULT_DURATION);
    }

    private void showUnmuteUsage(CommandSender sender) {
        Notification.info(sender, "用法:");
        Notification.info(sender, "/unmute <player> 取消禁言玩家");
        Notification.info(sender, "/unmute ip <player/ip> 取消禁言 IP");
    }

    private void showMuteList(CommandSender sender) {
        List<MuteManager.Entry> entries = EssentialsD.muteManager.getActiveMutes();
        if (entries.isEmpty()) {
            Notification.info(sender, "当前没有生效中的禁言");
            return;
        }

        Notification.info(sender, "当前生效中的禁言 (%d):", entries.size());
        for (MuteManager.Entry entry : entries) {
            String type = entry.targetType == MuteManager.TargetType.PLAYER ? "玩家" : "IP";
            Notification.info(sender, "[%s] %s | 剩余: %s", type, entry.getDisplayName(), entry.getRemainingText());
        }
    }

    private @Nullable MuteDuration.ParseResult resolveDuration(CommandSender sender, String[] args, int durationIndex) {
        String raw = args.length > durationIndex ? args[durationIndex] : EssentialsD.config.MUTE_DEFAULT_DURATION;
        MuteDuration.ParseResult result = MuteDuration.parse(raw);
        if (result.isValid()) {
            return result;
        }
        Notification.error(sender, "无效的时长格式: %s", raw);
        Notification.info(sender, "支持示例: 30m, 1h, 7d, permanent");
        return null;
    }

    private @Nullable ResolvedIpTarget resolveIpTarget(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            String ip = MuteManager.normalizeIp(MuteManager.getPlayerIp(online));
            if (ip != null && !ip.isBlank()) {
                return new ResolvedIpTarget(ip, online.getName());
            }
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(input);
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            String latestIp = LoginRecord.getLatestIp(offline.getUniqueId());
            if (latestIp != null && !latestIp.isBlank()) {
                String playerName = offline.getName();
                if (playerName == null || playerName.isBlank()) {
                    playerName = input;
                }
                return new ResolvedIpTarget(MuteManager.normalizeIp(latestIp), playerName);
            }
        }

        String normalized = MuteManager.normalizeIp(input);
        if (looksLikeIp(normalized)) {
            return new ResolvedIpTarget(normalized, normalized);
        }
        return null;
    }

    private boolean looksLikeIp(String input) {
        return input != null && input.matches("^[0-9a-fA-F:.]+$");
    }

    private List<String> filter(List<String> words, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return words.stream()
                .filter(word -> word != null && word.toLowerCase(Locale.ROOT).startsWith(lower))
                .distinct()
                .toList();
    }

    private record ResolvedIpTarget(String ip, String name) {
    }
}
