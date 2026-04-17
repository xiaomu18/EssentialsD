package et.xiaomu.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Kickall implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Notification.error(sender, "用法: /kickall <reason>");
            return true;
        }
        String reason = ChatColor.translateAlternateColorCodes('&', String.join(" ", args));

        Scheduler.runTask(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.equals(sender) || player.isOp()) {
                    continue;
                }
                player.kickPlayer(reason);
            }
        });

        Notification.info(sender, "正在清空服务器...");
        return true;
    }
}
