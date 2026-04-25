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
            Notification.error(sender, "该命令只能由玩家执行");
            return true;
        }
        if (args.length != 0) {
            Notification.info(player, "用法: /pure");
            return true;
        }

        boolean currentlyEnabled = EssentialsD.pureManager.isEnabled(player);
        if (!currentlyEnabled && !EssentialsD.pureManager.isFeatureEnabled()) {
            Notification.warn(player, "服务器当前未启用 Pure 纯净模式");
            return true;
        }

        boolean nextState = !currentlyEnabled;
        if (!ensureKnownPlayer(player)) {
            Notification.error(player, "纯净模式状态保存失败，请稍后重试");
            return true;
        }
        if (!EssentialsD.pureManager.setEnabled(player.getUniqueId(), nextState)) {
            Notification.error(player, "纯净模式状态保存失败，请稍后重试");
            return true;
        }

        if (nextState) {
            Notification.info(player, "已开启纯净模式，仅位于纯净列表中的玩家可与你互相看见公屏消息");
            Notification.info(player, "使用 /purelist <add|remove|clear|list> 管理纯净列表");
            return true;
        }

        Notification.info(player, "已关闭纯净模式，你将恢复看到所有玩家的公屏消息，且所有玩家也可看到你的消息");
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
