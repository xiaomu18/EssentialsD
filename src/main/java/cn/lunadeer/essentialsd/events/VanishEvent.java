package cn.lunadeer.essentialsd.events;

import cn.lunadeer.essentialsd.EssentialsD;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class VanishEvent implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        EssentialsD.vanishManager.handleJoin(event.getPlayer());
        if (EssentialsD.vanishManager.isVanished(event.getPlayer().getUniqueId())) {
            event.joinMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (EssentialsD.vanishManager.isVanished(event.getPlayer().getUniqueId())) {
            event.quitMessage(null);
        }
        EssentialsD.vanishManager.handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        EssentialsD.vanishManager.refreshForcedState(event.getPlayer(), event.getNewGameMode(), true);
    }
}
