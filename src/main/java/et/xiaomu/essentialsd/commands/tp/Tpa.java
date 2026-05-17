package et.xiaomu.essentialsd.commands.tp;

import et.xiaomu.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class Tpa implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            Notification.warnKey(sender, "messages.common.player_only_command");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 1) {
            if (isActionArgument(args[0])) {
                Notification.errorKey(player, "messages.tpa.usage");
                return true;
            }
            Player target = EssentialsD.instance.getServer().getPlayer(args[0]);
            if (target == null || EssentialsD.vanishManager.isHiddenFrom(player, target)) {
                Notification.warnKey(player, "messages.api.player_not_online", args[0]);
                return true;
            }
            EssentialsD.tpManager.tpaRequest(player, target);
            return true;
        }

        if (args.length == 2) {
            if ("accept".equalsIgnoreCase(args[0])) {
                UUID taskId = parseTaskId(player, args[1]);
                if (taskId == null) {
                    return true;
                }
                EssentialsD.tpManager.accept(player, taskId);
                return true;
            }
            if ("deny".equalsIgnoreCase(args[0])) {
                UUID taskId = parseTaskId(player, args[1]);
                if (taskId == null) {
                    return true;
                }
                EssentialsD.tpManager.deny(player, taskId);
                return true;
            }
        }

        Notification.errorKey(player, "messages.tpa.usage");
        return true;
    }

    private boolean isActionArgument(String arg) {
        return "accept".equalsIgnoreCase(arg) || "deny".equalsIgnoreCase(arg);
    }

    private UUID parseTaskId(Player player, String rawTaskId) {
        try {
            return UUID.fromString(rawTaskId);
        } catch (IllegalArgumentException ignored) {
            Notification.errorKey(player, "messages.tpa.invalid_request_id");
            return null;
        }
    }
}
