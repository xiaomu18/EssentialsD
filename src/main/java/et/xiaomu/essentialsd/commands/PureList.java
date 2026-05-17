package et.xiaomu.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.dtos.PlayerName;
import et.xiaomu.essentialsd.utils.PlayerLookup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

public class PureList implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Notification.errorKey(sender, "messages.pure.player_only");
            return true;
        }
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subCommand) {
            case "add" -> handleAdd(player, args);
            case "remove" -> handleRemove(player, args);
            case "clear" -> handleClear(player, args);
            case "list" -> handleList(player, args);
            default -> {
                sendUsage(player);
                yield true;
            }
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        if (args.length == 1) {
            return Stream.of("add", "remove", "clear", "list")
                    .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && ("add".equalsIgnoreCase(args[0]) || "remove".equalsIgnoreCase(args[0]))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.equalsIgnoreCase(player.getName()))
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        return List.of();
    }

    private boolean handleAdd(Player player, String[] args) {
        if (args.length != 2) {
            sendUsage(player);
            return true;
        }

        OfflinePlayer target = PlayerLookup.resolve(args[1]);
        if (target == null) {
            Notification.errorKey(player, "messages.pure_list.player_not_found", args[1]);
            return true;
        }
        if (player.getUniqueId().equals(target.getUniqueId())) {
            Notification.warnKey(player, "messages.pure_list.add_self_forbidden");
            return true;
        }
        if (EssentialsD.pureManager.isListed(player.getUniqueId(), target.getUniqueId())) {
            Notification.warnKey(player, "messages.pure_list.already_listed", PlayerLookup.displayName(target));
            return true;
        }
        if (!ensureKnownPlayer(player.getUniqueId(), player.getName())) {
            Notification.errorKey(player, "messages.pure_list.save_owner_failed");
            return true;
        }
        if (!ensureKnownPlayer(target.getUniqueId(), PlayerLookup.displayName(target))) {
            Notification.errorKey(player, "messages.pure_list.save_target_failed");
            return true;
        }
        if (!EssentialsD.pureManager.addToList(player.getUniqueId(), target.getUniqueId())) {
            Notification.errorKey(player, "messages.pure_list.add_failed");
            return true;
        }
        Notification.infoKey(player, "messages.pure_list.added", PlayerLookup.displayName(target));
        return true;
    }

    private boolean handleRemove(Player player, String[] args) {
        if (args.length != 2) {
            sendUsage(player);
            return true;
        }

        OfflinePlayer target = PlayerLookup.resolve(args[1]);
        if (target == null) {
            Notification.errorKey(player, "messages.pure_list.player_not_found", args[1]);
            return true;
        }
        if (!EssentialsD.pureManager.isListed(player.getUniqueId(), target.getUniqueId())) {
            Notification.warnKey(player, "messages.pure_list.not_listed", PlayerLookup.displayName(target));
            return true;
        }
        if (!EssentialsD.pureManager.removeFromList(player.getUniqueId(), target.getUniqueId())) {
            Notification.errorKey(player, "messages.pure_list.remove_failed");
            return true;
        }
        Notification.infoKey(player, "messages.pure_list.removed", PlayerLookup.displayName(target));
        return true;
    }

    private boolean handleClear(Player player, String[] args) {
        if (args.length != 1) {
            sendUsage(player);
            return true;
        }
        List<UUID> entries = EssentialsD.pureManager.getListedPlayers(player.getUniqueId());
        if (entries.isEmpty()) {
            Notification.warnKey(player, "messages.pure_list.already_empty");
            return true;
        }
        if (!EssentialsD.pureManager.clearList(player.getUniqueId())) {
            Notification.errorKey(player, "messages.pure_list.clear_failed");
            return true;
        }
        Notification.infoKey(player, "messages.pure_list.cleared");
        return true;
    }

    private boolean handleList(Player player, String[] args) {
        if (args.length != 1) {
            sendUsage(player);
            return true;
        }

        List<UUID> entries = EssentialsD.pureManager.getListedPlayers(player.getUniqueId()).stream()
                .sorted((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(resolveName(left), resolveName(right)))
                .toList();

        boolean enabled = EssentialsD.pureManager.isEnabled(player);
        Notification.info(player, Component.text(EssentialsD.localization.get("ui.pure_list.mode_label"), NamedTextColor.GRAY)
                .append(Component.text(enabled ? EssentialsD.localization.get("common.enabled") : EssentialsD.localization.get("common.disabled"),
                        enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(Component.text(EssentialsD.pureManager.isFeatureEnabled() ? "" : EssentialsD.localization.get("ui.pure_list.server_disabled_suffix"), NamedTextColor.GOLD)));
        if (entries.isEmpty()) {
            Notification.infoKey(player, "messages.pure_list.empty");
            return true;
        }

        Notification.info(player, Component.text(EssentialsD.localization.get("ui.pure_list.title"), NamedTextColor.GRAY)
                .append(Component.text("(" + entries.size() + ")", NamedTextColor.YELLOW)));
        for (UUID targetId : entries) {
            Notification.info(player, formatEntryLine(player.getUniqueId(), targetId));
        }
        return true;
    }

    private void sendUsage(Player player) {
        Notification.infoKey(player, "messages.pure_list.usage");
    }

    private boolean ensureKnownPlayer(UUID uuid, String fallbackName) {
        if (PlayerName.getName(uuid) != null) {
            return true;
        }
        return PlayerName.setName(uuid, fallbackName);
    }

    private String resolveName(UUID uuid) {
        String name = PlayerName.getName(uuid);
        if (name != null && !name.isBlank()) {
            return name;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String offlineName = player.getName();
        return offlineName != null && !offlineName.isBlank() ? offlineName : uuid.toString();
    }

    private Component formatEntryLine(UUID ownerId, UUID targetId) {
        boolean targetPureEnabled = EssentialsD.pureManager.isEnabled(targetId);
        boolean mutuallyVisible = EssentialsD.pureManager.canMutuallySee(ownerId, targetId);

        Component line = Component.text(" - ", NamedTextColor.DARK_GRAY)
                .append(Component.text(resolveName(targetId), NamedTextColor.YELLOW))
                .append(Component.text(" ", NamedTextColor.GRAY))
                .append(Component.text(mutuallyVisible ? EssentialsD.localization.get("ui.pure_list.entry_mutual_visible") : EssentialsD.localization.get("ui.pure_list.entry_not_mutual"),
                        mutuallyVisible ? NamedTextColor.GREEN : NamedTextColor.RED));

        if (targetPureEnabled) {
            line = line.append(Component.text(EssentialsD.localization.get("ui.pure_list.entry_pure_mode"), NamedTextColor.GREEN));
        }
        return line;
    }
}
