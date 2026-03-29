package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Heal implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && args.length == 0) {
            Notification.warn(sender, "必须指定一个玩家: /heal <玩家名>");
            return true;
        }

        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                Notification.warn(sender, "玩家 %s 未找到或不在线!", args[0]);
                return true;
            }
        } else {
            target = (Player) sender;
        }

        healPlayer(target);
        if (sender.equals(target)) {
            Notification.info(sender, "你已恢复全部生命值和饱食度!");
        } else {
            Notification.info(sender, "已恢复玩家 %s 的生命值和饱食度!", target.getName());
            Notification.info(target, "你的生命值和饱食度已被管理员恢复!");
        }
        return true;
    }

    private void healPlayer(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20F);
        player.setExhaustion(0F);
    }
}
