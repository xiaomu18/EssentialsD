package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.minecraftpluginutils.Notification;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Save implements CommandExecutor {
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      Notification.info(sender, "正在保存服务器存档...");
      EssentialsD.instance.getServer().savePlayers();
      EssentialsD.instance.getServer().getWorlds().forEach(World::save);
      Notification.info(sender, "服务器存档已保存");
      return true;
   }
}
