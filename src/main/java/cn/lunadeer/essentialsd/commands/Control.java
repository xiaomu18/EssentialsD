package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class Control implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Notification.info(sender, "用法: /essd <reload|version>");
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            EssentialsD.config.reload();
            EssentialsD.muteManager.reload();
            Notification.info(sender, "已重新读取配置文件");
            return true;
        }
        if ("version".equalsIgnoreCase(args[0])) {
            Notification.info(sender, "插件版本: %s", EssentialsD.instance.getPluginMeta().getVersion());
            Notification.info(sender, "编译日期: %s", EssentialsD.instance.getBuildDate());
            Notification.info(sender, "项目地址: %s", EssentialsD.instance.getProjectUrl());
            return true;
        }
        Notification.error(sender, "用法: /essd <reload|version>");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        return Stream.of("version", "reload")
                .filter(subCommand -> sender.isOp() || !"reload".equals(subCommand))
                .filter(subCommand -> subCommand.startsWith(args[0].toLowerCase(Locale.ROOT)))
                .toList();
    }
}
