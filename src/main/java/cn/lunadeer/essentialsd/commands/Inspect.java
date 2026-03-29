package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.managers.inspect.InspectManager;
import cn.lunadeer.utils.Notification;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class Inspect implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player viewer)) {
            Notification.error(sender, "只有玩家才能使用这个命令");
            return true;
        }
        if (args.length == 0) {
            Notification.error(sender, "用法: /inspect <玩家> [--ender]");
            return true;
        }

        String targetArg = null;
        boolean enderMode = false;
        for (String arg : args) {
            if ("--ender".equalsIgnoreCase(arg)) {
                enderMode = true;
                continue;
            }
            if (targetArg == null) {
                targetArg = arg;
                continue;
            }
            Notification.error(sender, "用法: /inspect <玩家> [--ender]");
            return true;
        }
        if (targetArg == null || targetArg.isBlank()) {
            Notification.error(sender, "用法: /inspect <玩家> [--ender]");
            return true;
        }

        OfflinePlayer target = resolveOfflinePlayer(targetArg);

        EssentialsD.inspectManager.openInspect(
                viewer,
                target,
                enderMode ? InspectManager.Mode.ENDER_CHEST : InspectManager.Mode.PLAYER_INVENTORY
        );
        return true;
    }

    private OfflinePlayer resolveOfflinePlayer(String input) {
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(input));
        } catch (IllegalArgumentException ignored) {
            return Bukkit.getOfflinePlayer(input);
        }
    }
}
