package et.xiaomu.essentialsd.commands;

import et.xiaomu.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Suicide implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Notification.warnKey(sender, "messages.common.player_only_command");
            return true;
        }
        player.setHealth(0.0D);
        player.setKiller(player);
        player.sendMessage(Component.text(EssentialsD.localization.get("messages.suicide.self"), NamedTextColor.GOLD));
        EssentialsD.instance.getServer().broadcast(Component.text(EssentialsD.localization.format("messages.suicide.broadcast", player.getName()), NamedTextColor.GOLD));
        return true;
    }
}
