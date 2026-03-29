package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class Gamemode implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1 || args.length > 2) {
            Notification.error(sender, "Usage: /gamemode <survival|creative|adventure|spectator> [player]");
            return true;
        }

        GameMode mode = parseMode(args[0]);
        if (mode == null) {
            Notification.error(sender, "Unknown game mode: %s", args[0]);
            return true;
        }
        if (!sender.hasPermission("essd.gamemode." + mode.name().toLowerCase(Locale.ROOT))) {
            Notification.error(sender, "You do not have permission to use that game mode.");
            return true;
        }
        if (args.length > 1 && !sender.hasPermission("essd.gamemode.other")) {
            Notification.error(sender, "You do not have permission to change another player's game mode.");
            return true;
        }

        Player target = Apis.getPlayerFromArg(sender, args, 1);
        if (target == null) {
            return true;
        }

        Scheduler.runTask(() -> {
            target.setGameMode(mode);
            String modeName = mode.name().toLowerCase(Locale.ROOT);
            if (sender.equals(target)) {
                Notification.info(target, "Your game mode is now %s.", modeName);
            } else {
                Notification.info(sender, "%s is now in %s.", target.getName(), modeName);
                Notification.info(target, "Your game mode is now %s.", modeName);
            }
        });
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Stream.of("survival", "creative", "adventure", "spectator")
                    .filter(mode -> mode.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && sender.hasPermission("essd.gamemode.other")) {
            return null;
        }
        return List.of();
    }

    private GameMode parseMode(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "0", "s", "survival" -> GameMode.SURVIVAL;
            case "1", "c", "creative" -> GameMode.CREATIVE;
            case "2", "a", "adventure" -> GameMode.ADVENTURE;
            case "3", "sp", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
    }
}
