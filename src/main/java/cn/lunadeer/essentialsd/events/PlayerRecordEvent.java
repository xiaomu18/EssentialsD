package cn.lunadeer.essentialsd.events;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.commands.Vanish;
import cn.lunadeer.essentialsd.dtos.LoginRecord;
import cn.lunadeer.essentialsd.dtos.NameRecord;
import cn.lunadeer.essentialsd.dtos.PlayerName;
import cn.lunadeer.minecraftpluginutils.Notification;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerRecordEvent implements Listener {
   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onPlayerLogin(PlayerLoginEvent event) {
      Player player = event.getPlayer();
      String last_name = PlayerName.getName(player.getUniqueId());
      if (last_name == null) {
         PlayerName.setName(player.getUniqueId(), player.getName());
         NameRecord.newNameRecord(player);
      } else if (!last_name.equals(player.getName())) {
         PlayerName.updateName(player.getUniqueId(), player.getName());
         NameRecord.newNameRecord(player);
      }

   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onPlayerLogout(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      LoginRecord.newLoginRecord(player);
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();

      if (((Vanish) EssentialsD.commands.get("vanish")).invList.contains(player.getUniqueId())) {
         for (Player player1 : Bukkit.getOnlinePlayers()) {
            if (player1.equals(player) || player1.isOp()) {
               continue;
            }
            player1.hidePlayer(EssentialsD.instance, player);
         }
         event.joinMessage(null);

         Notification.warn(player, "你仍处于隐身状态");
         ((Vanish) EssentialsD.commands.get("vanish")).invBossBar.addPlayer(player);
         return;
      }

      if (!player.isOp()) {
         for (UUID hideUUID : ((Vanish) EssentialsD.commands.get("vanish")).invList) {
            Player hidePlayer = Bukkit.getPlayer(hideUUID);
            if (hidePlayer != null) {
               player.hidePlayer(EssentialsD.instance, hidePlayer);
            }
         }
      }
   }
}
