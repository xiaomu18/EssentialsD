package et.xiaomu.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.dtos.PlayerName;
import et.xiaomu.essentialsd.utils.PlayerLookup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

public class PureList implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Notification.error(sender, "该命令只能由玩家执行");
            return true;
        }
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subCommand) {
            case "add" -> handleAdd(player, args);
            case "remove" -> handleRemove(player, args);
            case "clear" -> handleClear(player, args);
            case "list" -> handleList(player, args);
            default -> {
                sendUsage(player);
                yield true;
            }
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        if (args.length == 1) {
            return Stream.of("add", "remove", "clear", "list")
                    .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && ("add".equalsIgnoreCase(args[0]) || "remove".equalsIgnoreCase(args[0]))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.equalsIgnoreCase(player.getName()))
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        return List.of();
    }

    private boolean handleAdd(Player player, String[] args) {
        if (args.length != 2) {
            sendUsage(player);
            return true;
        }

        OfflinePlayer target = PlayerLookup.resolve(args[1]);
        if (target == null) {
            Notification.error(player, "未找到玩家 %s", args[1]);
            return true;
        }
        if (player.getUniqueId().equals(target.getUniqueId())) {
            Notification.warn(player, "无需将自己加入纯净列表");
            return true;
        }
        if (EssentialsD.pureManager.isListed(player.getUniqueId(), target.getUniqueId())) {
            Notification.warn(player, "%s 已在你的纯净列表中", PlayerLookup.displayName(target));
            return true;
        }
        if (!ensureKnownPlayer(player.getUniqueId(), player.getName())) {
            Notification.error(player, "无法保存你的纯净列表，请稍后重试");
            return true;
        }
        if (!ensureKnownPlayer(target.getUniqueId(), PlayerLookup.displayName(target))) {
            Notification.error(player, "无法保存该玩家到纯净列表，请稍后重试");
            return true;
        }
        if (!EssentialsD.pureManager.addToList(player.getUniqueId(), target.getUniqueId())) {
            Notification.error(player, "添加纯净列表失败，请稍后重试");
            return true;
        }
        Notification.info(player, "已将 %s 加入你的纯净列表", PlayerLookup.displayName(target));
        return true;
    }

    private boolean handleRemove(Player player, String[] args) {
        if (args.length != 2) {
            sendUsage(player);
            return true;
        }

        OfflinePlayer target = PlayerLookup.resolve(args[1]);
        if (target == null) {
            Notification.error(player, "未找到玩家 %s", args[1]);
            return true;
        }
        if (!EssentialsD.pureManager.isListed(player.getUniqueId(), target.getUniqueId())) {
            Notification.warn(player, "%s 不在你的纯净列表中", PlayerLookup.displayName(target));
            return true;
        }
        if (!EssentialsD.pureManager.removeFromList(player.getUniqueId(), target.getUniqueId())) {
            Notification.error(player, "移除纯净列表失败，请稍后重试");
            return true;
        }
        Notification.info(player, "已将 %s 从你的纯净列表移除", PlayerLookup.displayName(target));
        return true;
    }

    private boolean handleClear(Player player, String[] args) {
        if (args.length != 1) {
            sendUsage(player);
            return true;
        }
        List<UUID> entries = EssentialsD.pureManager.getListedPlayers(player.getUniqueId());
        if (entries.isEmpty()) {
            Notification.warn(player, "你的纯净列表已经是空的");
            return true;
        }
        if (!EssentialsD.pureManager.clearList(player.getUniqueId())) {
            Notification.error(player, "清空纯净列表失败，请稍后重试");
            return true;
        }
        Notification.info(player, "已清空你的纯净列表");
        return true;
    }

    private boolean handleList(Player player, String[] args) {
        if (args.length != 1) {
            sendUsage(player);
            return true;
        }

        List<UUID> entries = EssentialsD.pureManager.getListedPlayers(player.getUniqueId()).stream()
                .sorted((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(resolveName(left), resolveName(right)))
                .toList();

        boolean enabled = EssentialsD.pureManager.isEnabled(player);
        Notification.info(player, Component.text("纯净模式: ", NamedTextColor.GRAY)
                .append(Component.text(enabled ? "开启" : "关闭", enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(Component.text(EssentialsD.pureManager.isFeatureEnabled() ? "" : " [服务器未启用]", NamedTextColor.GOLD)));
        if (entries.isEmpty()) {
            Notification.info(player, "你的纯净列表当前为空");
            return true;
        }

        Notification.info(player, Component.text("纯净列表 ", NamedTextColor.GRAY)
                .append(Component.text("(" + entries.size() + ")", NamedTextColor.YELLOW)));
        for (UUID targetId : entries) {
            Notification.info(player, formatEntryLine(player.getUniqueId(), targetId));
        }
        return true;
    }

    private void sendUsage(Player player) {
        Notification.info(player, "用法: /purelist <add|remove|clear|list> [玩家]");
    }

    private boolean ensureKnownPlayer(UUID uuid, String fallbackName) {
        if (PlayerName.getName(uuid) != null) {
            return true;
        }
        return PlayerName.setName(uuid, fallbackName);
    }

    private String resolveName(UUID uuid) {
        String name = PlayerName.getName(uuid);
        if (name != null && !name.isBlank()) {
            return name;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String offlineName = player.getName();
        return offlineName != null && !offlineName.isBlank() ? offlineName : uuid.toString();
    }

    private Component formatEntryLine(UUID ownerId, UUID targetId) {
        boolean targetPureEnabled = EssentialsD.pureManager.isEnabled(targetId);
        boolean mutuallyVisible = EssentialsD.pureManager.canMutuallySee(ownerId, targetId);

        Component line = Component.text(" - ", NamedTextColor.DARK_GRAY)
                .append(Component.text(resolveName(targetId), NamedTextColor.YELLOW))
                .append(Component.text(" ", NamedTextColor.GRAY))
                .append(Component.text(mutuallyVisible ? "[互相可见]" : "[对方未添加你]",
                        mutuallyVisible ? NamedTextColor.GREEN : NamedTextColor.RED));

        if (targetPureEnabled) {
            line = line.append(Component.text("[纯净模式]", NamedTextColor.GREEN));
        }
        return line;
    }
}
