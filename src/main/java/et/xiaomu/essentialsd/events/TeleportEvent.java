package et.xiaomu.essentialsd.events;

import et.xiaomu.essentialsd.EssentialsD;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class TeleportEvent implements Listener {
    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        EssentialsD.tpManager.updateLastTpLocation(player);
    }
}
