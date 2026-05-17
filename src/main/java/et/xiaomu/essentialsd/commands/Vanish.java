package et.xiaomu.essentialsd.commands;

import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.managers.VanishManager;
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
            Notification.errorKey(sender, "messages.vanish.no_permission_list");
            return true;
        }
        List<Player> vanishedPlayers = EssentialsD.vanishManager.getVanishedPlayers();
        if (vanishedPlayers.isEmpty()) {
            Notification.infoKey(sender, "messages.vanish.none_online");
            return true;
        }

        Notification.info(sender, Component.text(EssentialsD.localization.get("ui.vanish.list_title"), NamedTextColor.GRAY)
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
                Notification.errorKey(sender, "messages.vanish.console_requires_player");
                return true;
            }
            target = player;
        } else if (args.length == 2) {
            if (!sender.hasPermission(VanishManager.PERMISSION_OTHER)) {
                Notification.errorKey(sender, "messages.vanish.no_permission_other");
                return true;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                Notification.errorKey(sender, "messages.api.player_not_online", args[1]);
                return true;
            }
        } else {
            sendUsage(sender);
            return true;
        }

        if (!enable && EssentialsD.vanishManager.isForced(target.getUniqueId())) {
            Notification.errorKey(sender, "messages.vanish.forced_cannot_disable", target.getName());
            if (!sender.equals(target)) {
                Notification.warnKey(target, "messages.vanish.disable_attempt_blocked_target", sender.getName());
            }
            return true;
        }

        boolean changed = EssentialsD.vanishManager.setManualVanished(target, enable);
        if (!changed) {
            if (EssentialsD.vanishManager.isManualVanished(target.getUniqueId()) == enable) {
                if (enable) {
                    Notification.warnKey(sender, "messages.vanish.already_enabled", sender.equals(target) ? EssentialsD.localization.get("common.self") : target.getName());
                } else {
                    Notification.warnKey(sender, "messages.vanish.already_disabled", sender.equals(target) ? EssentialsD.localization.get("common.self") : target.getName());
                }
            } else {
                Notification.errorKey(sender, "messages.vanish.persist_failed");
            }
            return true;
        }

        if (sender.equals(target)) {
            if (enable) {
                Notification.infoKey(target, EssentialsD.vanishManager.isForced(target.getUniqueId())
                        ? "messages.vanish.enabled_with_forced"
                        : "messages.vanish.enabled");
            } else {
                Notification.infoKey(target, "messages.vanish.disabled");
            }
            return true;
        }

        Notification.infoKey(sender, "messages.vanish.set_other_sender", target.getName(), enable ? EssentialsD.localization.get("common.on") : EssentialsD.localization.get("common.off"));
        Notification.infoKey(target, "messages.vanish.set_other_target", sender.getName(), enable ? EssentialsD.localization.get("common.on") : EssentialsD.localization.get("common.off"));
        return true;
    }

    private void sendUsage(CommandSender sender) {
        Notification.infoKey(sender, "messages.vanish.usage");
    }

    private void sendStatus(CommandSender sender, Player player) {
        boolean vanished = EssentialsD.vanishManager.isVanished(player);
        boolean forced = EssentialsD.vanishManager.isForced(player.getUniqueId());
        Component status = Component.text(EssentialsD.localization.get("ui.vanish.status_label"), NamedTextColor.GRAY)
                .append(Component.text(vanished ? EssentialsD.localization.get("ui.vanish.status_enabled") : EssentialsD.localization.get("ui.vanish.status_disabled"),
                        vanished ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(forced ? Component.text(EssentialsD.localization.get("ui.vanish.status_forced_suffix"), NamedTextColor.GOLD) : Component.empty());
        Notification.info(sender, status);
    }

    private Component formatPlayerLine(Player player) {
        Component line = Component.text(" - ", NamedTextColor.DARK_GRAY)
                .append(Component.text(player.getName(), NamedTextColor.YELLOW));
        if (EssentialsD.vanishManager.isForced(player.getUniqueId())) {
            line = line.append(Component.text(EssentialsD.localization.get("ui.vanish.status_forced_suffix"), NamedTextColor.GOLD));
        }
        if (EssentialsD.vanishManager.canChatWhileVanished(player)) {
            line = line.append(Component.text(EssentialsD.localization.get("ui.vanish.status_chat_suffix"), NamedTextColor.AQUA));
        }
        return line;
    }
}
