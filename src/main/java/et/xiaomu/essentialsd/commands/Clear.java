package et.xiaomu.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import et.xiaomu.essentialsd.managers.inspect.InspectManager;
import et.xiaomu.essentialsd.managers.inspect.OfflinePlayerDataAccess;
import et.xiaomu.essentialsd.utils.PlayerLookup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Clear implements TabExecutor {
    private static final String PERMISSION_CLEAR = "essd.clear";
    private static final String PERMISSION_CLEAR_OTHER = "essd.clear.other";
    private static final String PERMISSION_CLEAR_OFFLINE = "essd.clear.offline";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION_CLEAR)) {
            Notification.error(sender, "你没有权限使用这个命令");
            return true;
        }

        ParsedArgs parsed = ParsedArgs.parse(args);
        if (parsed == null) {
            Notification.error(sender, "用法: /clear [玩家] [--ender] [--confirm]");
            return true;
        }

        OfflinePlayer target;
        if (parsed.target() == null) {
            if (!(sender instanceof Player player)) {
                Notification.error(sender, "控制台执行时必须指定玩家");
                return true;
            }
            target = player;
        } else {
            target = PlayerLookup.resolve(parsed.target());
            if (target == null) {
                Notification.error(sender, "找不到玩家: %s", parsed.target());
                return true;
            }
        }

        boolean self = sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId());
        if (!self && !sender.hasPermission(PERMISSION_CLEAR_OTHER)) {
            Notification.error(sender, "你没有权限清空其他玩家的数据");
            return true;
        }
        if (!target.isOnline() && !sender.hasPermission(PERMISSION_CLEAR_OFFLINE)) {
            Notification.error(sender, "你没有权限清空离线玩家的数据");
            return true;
        }

        if (!parsed.confirm()) {
            promptConfirmation(sender, target, parsed.ender(), self);
            return true;
        }

        executeClear(sender, target, parsed.ender(), self);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> words = new ArrayList<>();
            words.add("--ender");
            words.add("--confirm");
            if (sender.hasPermission(PERMISSION_CLEAR_OTHER)) {
                for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    words.add(player.getName());
                }
            }
            return filter(words, args[0]);
        }
        if (args.length >= 2) {
            return filter(List.of("--ender", "--confirm"), args[args.length - 1]);
        }
        return List.of();
    }

    private void promptConfirmation(CommandSender sender, OfflinePlayer target, boolean ender, boolean self) {
        String containerName = ender ? "末影箱" : "背包";
        String confirmCommand = buildConfirmCommand(target, ender, self);
        String targetName = PlayerLookup.displayName(target);

        if (sender instanceof Player player) {
            TextComponent message = Component.text()
                    .append(Component.text(targetName + " 的" + containerName + "即将被清空! ", NamedTextColor.GOLD))
                    .append(Component.text("点击此处确认操作!", NamedTextColor.RED))
                    .clickEvent(ClickEvent.runCommand(confirmCommand))
                    .build();
            Notification.warn(player, message);
            return;
        }

        Notification.warn(sender, "%s 的%s即将被清空，请重新执行以下命令确认: %s", targetName, containerName, confirmCommand);
    }

    private void executeClear(CommandSender sender, OfflinePlayer target, boolean ender, boolean self) {
        String containerName = ender ? "末影箱" : "背包";
        String targetName = PlayerLookup.displayName(target);

        if (target.isOnline()) {
            clearOnline(sender, target, ender, self, containerName, targetName);
            return;
        }

        Scheduler.runTask(() -> {
            Player online = org.bukkit.Bukkit.getPlayer(target.getUniqueId());
            if (online != null && online.isOnline()) {
                clearOnline(sender, online, ender, self, containerName, targetName);
                return;
            }

            Scheduler.runTaskAsync(() -> {
                boolean success = OfflinePlayerDataAccess.clear(target, ender ? InspectManager.Mode.ENDER_CHEST : InspectManager.Mode.PLAYER_INVENTORY);
                dispatchToSender(sender, () -> {
                    if (!success) {
                        Notification.error(sender, "无法清空离线玩家 %s 的%s", targetName, containerName);
                        return;
                    }
                    Notification.info(sender, "已清空离线玩家 %s 的%s", targetName, containerName);
                });
            });
        });
    }

    private void clearOnline(CommandSender sender, OfflinePlayer target, boolean ender, boolean self, String containerName, String targetName) {
        Player online = target.getPlayer();
        if (online == null) {
            Notification.error(sender, "无法读取玩家 %s 的在线状态", targetName);
            return;
        }

        Scheduler.runEntityTask(online, () -> {
            if (!online.isOnline()) {
                dispatchToSender(sender, () -> Notification.error(sender, "玩家 %s 已下线", targetName));
                return;
            }

            if (ender) {
                online.getEnderChest().clear();
            } else {
                clearPlayerInventory(online.getInventory());
            }
            online.updateInventory();

            dispatchToSender(sender, () -> {
                if (self) {
                    Notification.info(sender, "已清空你的%s", containerName);
                } else {
                    Notification.info(sender, "已清空 %s 的%s", targetName, containerName);
                }
            });

            if (!self) {
                Notification.warn(online, "你的%s已被 %s 清空", containerName, sender.getName());
            }
        });
    }

    private void clearPlayerInventory(PlayerInventory inventory) {
        inventory.setStorageContents(new ItemStack[36]);
        inventory.setArmorContents(new ItemStack[4]);
        inventory.setItemInOffHand(null);
    }

    private void dispatchToSender(CommandSender sender, Runnable action) {
        if (sender instanceof Player player) {
            Scheduler.runEntityTask(player, action);
            return;
        }
        Scheduler.runTask(action);
    }

    private String buildConfirmCommand(OfflinePlayer target, boolean ender, boolean self) {
        StringBuilder builder = new StringBuilder("/clear ");
        if (!self) {
            builder.append(target.getUniqueId()).append(" ");
        }
        if (ender) {
            builder.append("--ender ");
        }
        builder.append("--confirm");
        return builder.toString().trim();
    }

    private List<String> filter(List<String> words, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return words.stream()
                .filter(word -> word != null && word.toLowerCase(Locale.ROOT).startsWith(lower))
                .distinct()
                .toList();
    }

    private record ParsedArgs(@Nullable String target, boolean ender, boolean confirm) {
        private static @Nullable ParsedArgs parse(String[] args) {
            String target = null;
            boolean ender = false;
            boolean confirm = false;

            for (String arg : args) {
                if ("--ender".equalsIgnoreCase(arg)) {
                    ender = true;
                    continue;
                }
                if ("--confirm".equalsIgnoreCase(arg)) {
                    confirm = true;
                    continue;
                }
                if (target == null) {
                    target = arg;
                    continue;
                }
                return null;
            }
            return new ParsedArgs(target, ender, confirm);
        }
    }
}
