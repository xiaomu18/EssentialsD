package et.xiaomu.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class More implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            Notification.warn(sender, "只有玩家可以使用此命令");
            return true;
        }

        if (args.length > 1) {
            Notification.error(sender, "用法: /more [amount]");
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            Notification.error(sender, "你手上没有物品");
            return true;
        }

        int maxAmount = item.getMaxStackSize();
        int targetAmount = maxAmount;
        if (args.length == 1) {
            try {
                targetAmount = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                Notification.error(sender, "参数错误，数量必须是 1-%d 之间的整数", maxAmount);
                return true;
            }
            if (targetAmount < 1 || targetAmount > maxAmount) {
                Notification.error(sender, "参数错误，数量必须是 1-%d 之间的整数", maxAmount);
                return true;
            }
        }

        item.setAmount(targetAmount);
        player.getInventory().setItemInMainHand(item);
        Notification.info(sender, "已将手持物品数量设置为 %d", targetAmount);
        return true;
    }
}
