package cn.lunadeer.essentialsd.commands.warp;

import cn.lunadeer.essentialsd.tuis.WarpList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Warps implements CommandExecutor {
   public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
      WarpList.show(commandSender, strings);
      return true;
   }
}
