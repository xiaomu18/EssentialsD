package et.xiaomu.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import et.xiaomu.essentialsd.EssentialsD;
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
            Notification.errorKey(sender, "messages.clear.no_permission");
            return true;
        }

        ParsedArgs parsed = ParsedArgs.parse(args);
        if (parsed == null) {
            Notification.errorKey(sender, "messages.clear.usage");
            return true;
        }

        OfflinePlayer target;
        if (parsed.target() == null) {
            if (!(sender instanceof Player player)) {
                Notification.errorKey(sender, "messages.clear.console_requires_player");
                return true;
            }
            target = player;
        } else {
            target = PlayerLookup.resolve(parsed.target());
            if (target == null) {
                Notification.errorKey(sender, "messages.clear.player_not_found", parsed.target());
                return true;
            }
        }

        boolean self = sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId());
        if (!self && !sender.hasPermission(PERMISSION_CLEAR_OTHER)) {
            Notification.errorKey(sender, "messages.clear.no_permission_other");
            return true;
        }
        if (!target.isOnline() && !sender.hasPermission(PERMISSION_CLEAR_OFFLINE)) {
            Notification.errorKey(sender, "messages.clear.no_permission_offline");
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
        String containerName = containerName(ender);
        String confirmCommand = buildConfirmCommand(target, ender, self);
        String targetName = PlayerLookup.displayName(target);

        if (sender instanceof Player player) {
            TextComponent message = Component.text()
                    .append(Component.text(EssentialsD.localization.format("messages.clear.confirm_prompt", targetName, containerName), NamedTextColor.GOLD))
                    .append(Component.text(EssentialsD.localization.get("messages.clear.click_to_confirm"), NamedTextColor.RED))
                    .clickEvent(ClickEvent.runCommand(confirmCommand))
                    .build();
            Notification.warn(player, message);
            return;
        }

        Notification.warnKey(sender, "messages.clear.confirm_console", targetName, containerName, confirmCommand);
    }

    private void executeClear(CommandSender sender, OfflinePlayer target, boolean ender, boolean self) {
        String containerName = containerName(ender);
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
                        Notification.errorKey(sender, "messages.clear.offline_failed", targetName, containerName);
                        return;
                    }
                    Notification.infoKey(sender, "messages.clear.offline_done", targetName, containerName);
                });
            });
        });
    }

    private void clearOnline(CommandSender sender, OfflinePlayer target, boolean ender, boolean self, String containerName, String targetName) {
        Player online = target.getPlayer();
        if (online == null) {
            Notification.errorKey(sender, "messages.clear.read_online_failed", targetName);
            return;
        }

        Scheduler.runEntityTask(online, () -> {
            if (!online.isOnline()) {
                dispatchToSender(sender, () -> Notification.errorKey(sender, "messages.clear.player_logged_out", targetName));
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
                    Notification.infoKey(sender, "messages.clear.self_done", containerName);
                } else {
                    Notification.infoKey(sender, "messages.clear.other_done", targetName, containerName);
                }
            });

            if (!self) {
                Notification.warnKey(online, "messages.clear.target_notified", containerName, sender.getName());
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

    private String containerName(boolean ender) {
        return ender ? EssentialsD.localization.get("ui.clear.ender_chest") : EssentialsD.localization.get("ui.clear.inventory");
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
