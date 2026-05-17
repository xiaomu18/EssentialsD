package et.xiaomu.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import et.xiaomu.essentialsd.EssentialsD;
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
            Notification.errorKey(sender, "messages.gamemode.usage");
            return true;
        }

        GameMode mode = parseMode(args[0]);
        if (mode == null) {
            Notification.errorKey(sender, "messages.gamemode.unknown_mode", args[0]);
            return true;
        }
        if (!sender.hasPermission("essd.gamemode." + mode.name().toLowerCase(Locale.ROOT))) {
            Notification.errorKey(sender, "messages.gamemode.no_permission_mode");
            return true;
        }
        if (args.length > 1 && !sender.hasPermission("essd.gamemode.other")) {
            Notification.errorKey(sender, "messages.gamemode.no_permission_other");
            return true;
        }

        Player target = Apis.getPlayerFromArg(sender, args, 1);
        if (target == null) {
            return true;
        }

        Scheduler.runTask(() -> {
            target.setGameMode(mode);
            String modeName = EssentialsD.localization.get("common.gamemode." + mode.name().toLowerCase(Locale.ROOT));
            if (sender.equals(target)) {
                Notification.infoKey(target, "messages.gamemode.set_self", modeName);
            } else {
                Notification.infoKey(sender, "messages.gamemode.set_other_sender", target.getName(), modeName);
                Notification.infoKey(target, "messages.gamemode.set_self", modeName);
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
