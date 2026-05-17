package et.xiaomu.essentialsd.commands;

import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.managers.inspect.InspectManager;
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
    private static final String PERMISSION_INSPECT = "essd.inspect";
    private static final String PERMISSION_INSPECT_WRITE = "essd.inspect.write";
    private static final String PERMISSION_INSPECT_OFFLINE = "essd.inspect.offline";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player viewer)) {
            Notification.errorKey(sender, "messages.common.player_only_command");
            return true;
        }
        if (args.length == 0) {
            Notification.errorKey(sender, "messages.inspect.usage");
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
            Notification.errorKey(sender, "messages.inspect.usage");
            return true;
        }
        if (targetArg == null || targetArg.isBlank()) {
            Notification.errorKey(sender, "messages.inspect.usage");
            return true;
        }
        if (!viewer.hasPermission(PERMISSION_INSPECT)) {
            Notification.errorKey(viewer, "messages.inspect.no_permission");
            return true;
        }

        OfflinePlayer target = resolveOfflinePlayer(targetArg);
        boolean writable = false;
        if (target.isOnline()) {
            writable = viewer.hasPermission(PERMISSION_INSPECT_WRITE);
        } else if (!viewer.hasPermission(PERMISSION_INSPECT_OFFLINE)) {
            Notification.errorKey(viewer, "messages.inspect.no_permission_offline");
            return true;
        }

        EssentialsD.inspectManager.openInspect(
                viewer,
                target,
                enderMode ? InspectManager.Mode.ENDER_CHEST : InspectManager.Mode.PLAYER_INVENTORY,
                writable
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
