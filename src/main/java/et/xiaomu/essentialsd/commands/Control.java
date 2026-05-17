package et.xiaomu.essentialsd.commands;

import et.xiaomu.essentialsd.EssentialsD;
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
    private static final List<String> RELOAD_TARGETS = List.of("chat", "locale", "config");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Notification.infoKey(sender, "messages.config.usage");
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            if (args.length == 1) {
                EssentialsD.config.reload();
                EssentialsD.chatAntiSpamManager.reset();
                EssentialsD.localization.reload();
                Notification.reloadPrefix();
                EssentialsD.muteManager.reload();
                EssentialsD.vanishManager.reload();
                Notification.infoKey(sender, "messages.config.reloaded_all");
                return true;
            }
            if (args.length == 2) {
                String target = args[1].toLowerCase(Locale.ROOT);
                switch (target) {
                    case "chat" -> {
                        EssentialsD.config.reloadChatOnly();
                        EssentialsD.chatAntiSpamManager.reset();
                        Notification.infoKey(sender, "messages.config.reloaded_chat");
                        return true;
                    }
                    case "locale" -> {
                        EssentialsD.localization.reload();
                        Notification.reloadPrefix();
                        EssentialsD.vanishManager.reload();
                        Notification.infoKey(sender, "messages.config.reloaded_locale");
                        return true;
                    }
                    case "config" -> {
                        EssentialsD.config.reloadConfigOnly();
                        EssentialsD.vanishManager.reload();
                        Notification.infoKey(sender, "messages.config.reloaded_config");
                        return true;
                    }
                    default -> {
                        Notification.errorKey(sender, "messages.config.usage");
                        return true;
                    }
                }
            }
            Notification.errorKey(sender, "messages.config.usage");
            return true;
        }
        if ("version".equalsIgnoreCase(args[0])) {
            Notification.info(sender, "This server is running EssentialsD v%s %s@%s (%s) (Implementing API version %s)", EssentialsD.instance.getPluginMeta().getVersion(), EssentialsD.gitBranch, EssentialsD.instance.getGitCommit(), EssentialsD.instance.getBuildDate(), EssentialsD.instance.getPluginMeta().getAPIVersion());
            Notification.info(sender, "Author: lunadeer (previous), xiaomu18 (now)");
            Notification.info(sender, EssentialsD.instance.getProjectUrl());
            return true;
        }
        Notification.errorKey(sender, "messages.config.usage");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Stream.of("version", "reload")
                    .filter(subCommand -> sender.isOp() || !"reload".equals(subCommand))
                    .filter(subCommand -> subCommand.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && "reload".equalsIgnoreCase(args[0]) && (sender.isOp() || sender.hasPermission("essd.control"))) {
            String lower = args[1].toLowerCase(Locale.ROOT);
            return RELOAD_TARGETS.stream()
                    .filter(target -> target.startsWith(lower))
                    .toList();
        }
        return List.of();
    }
}
