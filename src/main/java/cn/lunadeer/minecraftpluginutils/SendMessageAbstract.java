package cn.lunadeer.minecraftpluginutils;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SendMessageAbstract {
   private BukkitAudiences adventure = null;
   private JavaPlugin plugin;

   public SendMessageAbstract(JavaPlugin plugin) {
      this.plugin = plugin;
      if (!Common.isPaper()) {
         this.adventure = BukkitAudiences.create(plugin);
      }

   }

   public void sendMessage(Player player, Component msg) {
      if (this.adventure == null) {
         player.sendMessage(msg);
      } else {
         this.adventure.player(player).sendMessage(msg);
      }

   }

   public void sendMessage(CommandSender sender, Component msg) {
      if (this.adventure == null) {
         sender.sendMessage(msg);
      } else {
         this.adventure.sender(sender).sendMessage(msg);
      }

   }

   public void broadcast(Component msg) {
      if (this.adventure == null) {
         this.plugin.getServer().broadcast(msg);
      } else {
         this.adventure.all().sendMessage(msg);
      }

   }

   public void sendActionBar(Player player, Component msg) {
      if (this.adventure == null) {
         player.sendActionBar(msg);
      } else {
         this.adventure.player(player).sendActionBar(msg);
      }

   }
}
