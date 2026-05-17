package et.xiaomu.essentialsd.commands;

import cn.lunadeer.utils.Notification;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

public class Hat implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            Notification.warnKey(sender, "messages.common.player_only_command");
            return true;
        } else {
            Player player = (Player) sender;
            PlayerInventory inventory = player.getInventory();
            ItemStack right_hand_item = inventory.getItemInMainHand();
            if (right_hand_item.getType().isAir()) {
                Notification.warnKey(player, "messages.show_item.empty_hand");
                return true;
            } else {
                ItemStack head_item = inventory.getHelmet();
                inventory.setItemInMainHand(head_item);
                inventory.setHelmet(right_hand_item);
                Notification.infoKey(player, "messages.hat.success");
                return true;
            }
        }
    }
}
