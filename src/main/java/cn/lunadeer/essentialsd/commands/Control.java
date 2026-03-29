package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Control implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Notification.info(sender, "用法: /essd reload");
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            EssentialsD.config.reload();
            Notification.info(sender, "已重新读取配置文件");
        }
        return true;
    }
}
