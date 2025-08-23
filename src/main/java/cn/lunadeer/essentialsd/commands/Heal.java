package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.minecraftpluginutils.Notification;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Heal implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 处理 heal 命令
        if (!(sender instanceof Player) && args.length == 0) {
            Notification.warn(sender, "必须指定一个玩家: /heal <玩家名>");
            return true;
        }

        Player target;
        if (args.length > 0) {
            // 尝试获取指定玩家
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                Notification.warn(sender, "玩家 " + args[0] + " 未找到或不在线!");
                return true;
            }
        } else {
            // 没有参数，目标是命令发送者自己
            target = (Player) sender;
        }

        // 执行治疗
        healPlayer(target);

        // 发送消息
        if (sender.equals(target)) {
            Notification.info(sender, "你已恢复全部生命值和饱食度!");
        } else {
            Notification.info(sender, "已恢复玩家 " + target.getName() + " 的生命值和饱食度!");
            Notification.info(target, "你的生命值和饱食度已被管理员恢复!");
        }
        return true;
    }

    private void healPlayer(Player player) {
        // 恢复生命值到最大值
        player.setHealth(player.getMaxHealth());

        // 恢复饱食度到最大值 (20)
        player.setFoodLevel(20);

        // 恢复饱和度到最大值 (20)
        player.setSaturation(20f);

        // 清除饥饿效果（可选）
        player.setExhaustion(0f);

        // 清除所有药水效果（可选，如果需要的话）
        // player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    }
}
