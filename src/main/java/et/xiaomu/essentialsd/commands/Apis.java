package et.xiaomu.essentialsd.commands;

import et.xiaomu.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import org.bukkit.World.Environment;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class Apis {
    public static Player getPlayerFromArg(CommandSender sender, String[] args, int pos) {
        if (args.length <= pos) {
            if (sender instanceof Player) {
                return (Player) sender;
            } else {
                Notification.error(sender, "以控制台身份执行时，必须指定玩家");
                return null;
            }
        } else {
            Player target = EssentialsD.instance.getServer().getPlayer(args[pos]);
            if (target == null) {
                Notification.warn(sender, "玩家 %s 不在线", args[0]);
                return null;
            } else {
                return target;
            }
        }
    }

    public static List playerNames() {
        return (List) EssentialsD.instance.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    public static void setOverWorldTime(CommandSender sender, long time) {
        Scheduler.runTask(() -> EssentialsD.instance.getServer().getWorlds().forEach((world) -> {
            if (world.getEnvironment() == Environment.NORMAL) {
                world.setTime(time);
                Notification.info(sender, "设置 %s 时间为 %d", world.getName(), time);
            }

        }));
    }
}
