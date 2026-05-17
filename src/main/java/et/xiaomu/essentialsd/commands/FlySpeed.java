package et.xiaomu.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import et.xiaomu.essentialsd.EssentialsD;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class FlySpeed implements TabExecutor {
    private static final float DEFAULT_FLY_SPEED = 0.1F;
    private static final double MIN_DISPLAY_SPEED = 0.0D;
    private static final double MAX_DISPLAY_SPEED = 10.0D;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1 || args.length > 2) {
            Notification.errorKey(sender, "messages.flyspeed.usage");
            return true;
        }
        if (!(sender instanceof Player) && args.length < 2) {
            Notification.errorKey(sender, "messages.flyspeed.console_requires_player");
            return true;
        }
        if (args.length == 2 && !sender.hasPermission("essd.flyspeed.other")) {
            Notification.errorKey(sender, "messages.flyspeed.no_permission_other");
            return true;
        }

        Float flySpeed = parseFlySpeed(args[0]);
        if (flySpeed == null) {
            Notification.errorKey(sender, "messages.flyspeed.invalid_value");
            return true;
        }

        Player target = Apis.getPlayerFromArg(sender, args, 1);
        if (target == null) {
            return true;
        }

        String displayValue = "reset".equalsIgnoreCase(args[0]) ? EssentialsD.localization.get("common.default_value") : args[0];
        Scheduler.runEntityTask(target, () -> {
            target.setFlySpeed(flySpeed);
            if (sender.equals(target)) {
                Notification.infoKey(target, "messages.flyspeed.set_self", displayValue);
                return;
            }
            Notification.infoKey(sender, "messages.flyspeed.set_other_sender", target.getName(), displayValue);
            Notification.infoKey(target, "messages.flyspeed.set_other_target", sender.getName(), displayValue);
        });
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reset", "1.0", "1.1").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && sender.hasPermission("essd.flyspeed.other")) {
            String input = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(input))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        return List.of();
    }

    private Float parseFlySpeed(String rawValue) {
        if ("reset".equalsIgnoreCase(rawValue)) {
            return DEFAULT_FLY_SPEED;
        }
        try {
            double displaySpeed = Double.parseDouble(rawValue);
            if (!Double.isFinite(displaySpeed) || displaySpeed < MIN_DISPLAY_SPEED || displaySpeed > MAX_DISPLAY_SPEED) {
                return null;
            }
            return (float) (displaySpeed / 10.0D);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
