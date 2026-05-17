package et.xiaomu.essentialsd.commands;

import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.dtos.LoginRecord;
import et.xiaomu.essentialsd.managers.MuteManager;
import et.xiaomu.essentialsd.utils.MuteDuration;
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
                Notification.errorKey(sender, "messages.mute.resolve_ip_failed");
                return true;
            }

            EssentialsD.muteManager.muteIp(resolved.ip(), resolved.name(), duration.getDurationMillis(), sender.getName());
            Notification.warnKey(sender, "messages.mute.ip_muted", resolved.ip(), MuteDuration.formatDuration(duration.getDurationMillis()));
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
        Notification.warnKey(sender, "messages.mute.player_muted", playerName, MuteDuration.formatDuration(duration.getDurationMillis()));
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
                Notification.errorKey(sender, "messages.mute.resolve_ip_simple_failed");
                return true;
            }

            if (EssentialsD.muteManager.unmuteIp(resolved.ip())) {
                Notification.infoKey(sender, "messages.mute.ip_unmuted", resolved.ip());
            } else {
                Notification.errorKey(sender, "messages.mute.unmute_failed");
            }
            return true;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
        if (EssentialsD.muteManager.unmutePlayer(player.getUniqueId())) {
            String playerName = player.getName();
            if (playerName == null || playerName.isBlank()) {
                playerName = player.getUniqueId().toString();
            }
            Notification.infoKey(sender, "messages.mute.player_unmuted", playerName);
        } else {
            Notification.errorKey(sender, "messages.mute.unmute_failed");
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
        Notification.infoKey(sender, "messages.mute.usage_header");
        Notification.infoKey(sender, "messages.mute.usage_list");
        Notification.infoKey(sender, "messages.mute.usage_player");
        Notification.infoKey(sender, "messages.mute.usage_ip");
        Notification.infoKey(sender, "messages.mute.usage_duration_examples", EssentialsD.config.MUTE_DEFAULT_DURATION);
    }

    private void showUnmuteUsage(CommandSender sender) {
        Notification.infoKey(sender, "messages.mute.usage_header");
        Notification.infoKey(sender, "messages.mute.unmute_usage_player");
        Notification.infoKey(sender, "messages.mute.unmute_usage_ip");
    }

    private void showMuteList(CommandSender sender) {
        List<MuteManager.Entry> entries = EssentialsD.muteManager.getActiveMutes();
        if (entries.isEmpty()) {
            Notification.infoKey(sender, "messages.mute.list_empty");
            return;
        }

        Notification.infoKey(sender, "messages.mute.list_header", entries.size());
        for (MuteManager.Entry entry : entries) {
            String type = entry.targetType == MuteManager.TargetType.PLAYER
                    ? EssentialsD.localization.get("ui.mute.type_player")
                    : EssentialsD.localization.get("ui.mute.type_ip");
            Notification.infoKey(sender, "messages.mute.list_entry", type, entry.getDisplayName(), entry.getRemainingText());
        }
    }

    private @Nullable MuteDuration.ParseResult resolveDuration(CommandSender sender, String[] args, int durationIndex) {
        String raw = args.length > durationIndex ? args[durationIndex] : EssentialsD.config.MUTE_DEFAULT_DURATION;
        MuteDuration.ParseResult result = MuteDuration.parse(raw);
        if (result.isValid()) {
            return result;
        }
        Notification.errorKey(sender, "messages.mute.invalid_duration", raw);
        Notification.infoKey(sender, "messages.mute.duration_examples");
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
