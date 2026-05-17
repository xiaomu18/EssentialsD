package et.xiaomu.essentialsd.commands;

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
            Notification.warnKey(sender, "messages.heal.console_requires_player");
            return true;
        }

        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                Notification.warnKey(sender, "messages.heal.player_not_found", args[0]);
                return true;
            }
        } else {
            target = (Player) sender;
        }

        healPlayer(target);
        if (sender.equals(target)) {
            Notification.infoKey(sender, "messages.heal.healed_self");
        } else {
            Notification.infoKey(sender, "messages.heal.healed_other_sender", target.getName());
            Notification.infoKey(target, "messages.heal.healed_other_target");
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
