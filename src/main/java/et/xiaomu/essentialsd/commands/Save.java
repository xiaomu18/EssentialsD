package et.xiaomu.essentialsd.commands;

import et.xiaomu.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Save implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Notification.infoKey(sender, "messages.save.start");
        EssentialsD.instance.getServer().savePlayers();
        EssentialsD.instance.getServer().getWorlds().forEach(World::save);
        Notification.infoKey(sender, "messages.save.done");
        return true;
    }
}
