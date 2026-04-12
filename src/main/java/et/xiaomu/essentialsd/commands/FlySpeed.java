package et.xiaomu.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
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
            Notification.error(sender, "用法: /flyspeed <reset|速度倍率> [玩家]");
            return true;
        }
        if (!(sender instanceof Player) && args.length < 2) {
            Notification.error(sender, "控制台必须指定目标玩家");
            return true;
        }
        if (args.length == 2 && !sender.hasPermission("essd.flyspeed.other")) {
            Notification.error(sender, "你没有权限修改其他玩家的飞行速度");
            return true;
        }

        Float flySpeed = parseFlySpeed(args[0]);
        if (flySpeed == null) {
            Notification.error(sender, "参数错误，速度倍率必须是 reset 或 0.0 到 10.0 之间的小数");
            return true;
        }

        Player target = Apis.getPlayerFromArg(sender, args, 1);
        if (target == null) {
            return true;
        }

        String displayValue = "reset".equalsIgnoreCase(args[0]) ? "默认值" : args[0];
        Scheduler.runEntityTask(target, () -> {
            target.setFlySpeed(flySpeed);
            if (sender.equals(target)) {
                Notification.info(target, "已将飞行速度设置为 %s", displayValue);
                return;
            }
            Notification.info(sender, "已将 %s 的飞行速度设置为 %s", target.getName(), displayValue);
            Notification.info(target, "%s 将你的飞行速度设置为 %s", sender.getName(), displayValue);
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
