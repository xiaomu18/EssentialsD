package et.xiaomu.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.dtos.PlayerName;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Pure implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Notification.errorKey(sender, "messages.pure.player_only");
            return true;
        }
        if (args.length != 0) {
            Notification.infoKey(player, "messages.pure.usage");
            return true;
        }

        boolean currentlyEnabled = EssentialsD.pureManager.isEnabled(player);
        if (!currentlyEnabled && !EssentialsD.pureManager.isFeatureEnabled()) {
            Notification.warnKey(player, "messages.pure.feature_disabled");
            return true;
        }

        boolean nextState = !currentlyEnabled;
        if (!ensureKnownPlayer(player)) {
            Notification.errorKey(player, "messages.pure.save_failed");
            return true;
        }
        if (!EssentialsD.pureManager.setEnabled(player.getUniqueId(), nextState)) {
            Notification.errorKey(player, "messages.pure.save_failed");
            return true;
        }

        if (nextState) {
            Notification.infoKey(player, "messages.pure.enabled");
            Notification.infoKey(player, "messages.pure.manage_hint");
            return true;
        }

        Notification.infoKey(player, "messages.pure.disabled");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }

    private boolean ensureKnownPlayer(Player player) {
        String existing = PlayerName.getName(player.getUniqueId());
        if (existing != null && !existing.isBlank()) {
            return true;
        }
        return PlayerName.setName(player.getUniqueId(), player.getName());
    }
}
