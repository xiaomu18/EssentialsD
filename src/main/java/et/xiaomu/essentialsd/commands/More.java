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
            Notification.warnKey(sender, "messages.common.player_only_command");
            return true;
        }

        if (args.length > 1) {
            Notification.errorKey(sender, "messages.more.usage");
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            Notification.errorKey(sender, "messages.more.empty_hand");
            return true;
        }

        int maxAmount = item.getMaxStackSize();
        int targetAmount = maxAmount;
        if (args.length == 1) {
            try {
                targetAmount = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                Notification.errorKey(sender, "messages.more.invalid_amount", maxAmount);
                return true;
            }
            if (targetAmount < 1 || targetAmount > maxAmount) {
                Notification.errorKey(sender, "messages.more.invalid_amount", maxAmount);
                return true;
            }
        }

        item.setAmount(targetAmount);
        player.getInventory().setItemInMainHand(item);
        Notification.infoKey(sender, "messages.more.updated", targetAmount);
        return true;
    }
}
