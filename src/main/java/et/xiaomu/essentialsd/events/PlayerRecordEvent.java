package et.xiaomu.essentialsd.events;

import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.commands.Kickall;
import et.xiaomu.essentialsd.dtos.LoginRecord;
import et.xiaomu.essentialsd.dtos.NameRecord;
import et.xiaomu.essentialsd.dtos.PlayerName;
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
