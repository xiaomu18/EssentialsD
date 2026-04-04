package et.xiaomu.essentialsd.commands;

import et.xiaomu.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Suicide implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Notification.warn(sender, "只有玩家可以使用此命令");
            return true;
        }
        player.setHealth(0.0D);
        player.setKiller(player);
        player.sendMessage("§6再见了, 这个令人伤心的世界!");
        EssentialsD.instance.getServer().broadcastMessage("§6" + player.getName() + "结束了自己的生命");
        return true;
    }
}
