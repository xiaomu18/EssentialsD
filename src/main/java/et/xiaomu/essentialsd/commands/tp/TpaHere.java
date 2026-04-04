package et.xiaomu.essentialsd.commands.tp;

import et.xiaomu.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpaHere implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            Notification.warn(sender, "只有玩家可以使用此命令");
            return true;
        } else {
            Player player = (Player) sender;
            if (args.length == 1) {
                if (EssentialsD.vanishManager.isVanished(player) && !EssentialsD.vanishManager.canTpahereWhileVanished(player)) {
                    Notification.warn(player, "你当前处于隐身状态，不能发送 tpahere 请求");
                    return true;
                }
                Player target = EssentialsD.instance.getServer().getPlayer(args[0]);
                if (target == null || EssentialsD.vanishManager.isHiddenFrom(player, target)) {
                    Notification.warn(player, "玩家 %s 不在线", args[0]);
                    return true;
                } else {
                    EssentialsD.tpManager.tpahereRequest(player, target);
                    return true;
                }
            } else {
                Notification.error(player, "参数错误");
                return false;
            }
        }
    }
}
