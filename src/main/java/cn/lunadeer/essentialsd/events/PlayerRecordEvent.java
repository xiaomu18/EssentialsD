package cn.lunadeer.essentialsd.events;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.commands.Kickall;
import cn.lunadeer.essentialsd.dtos.LoginRecord;
import cn.lunadeer.essentialsd.dtos.NameRecord;
import cn.lunadeer.essentialsd.dtos.PlayerName;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerRecordEvent implements Listener {
    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        if (((Kickall) EssentialsD.commands.get("kickall")).duration > System.currentTimeMillis() && !player.isOp()) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ((Kickall) EssentialsD.commands.get("kickall")).reason);
            return;
        }

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
}
