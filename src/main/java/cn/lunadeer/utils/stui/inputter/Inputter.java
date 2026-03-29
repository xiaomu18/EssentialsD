package cn.lunadeer.utils.stui.inputter;

import cn.lunadeer.utils.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class Inputter implements Listener {

    private static Inputter instance;

    public static Inputter getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Inputter has not been initialized. Please call Inputter.init(plugin) first.");
        }
        return instance;
    }

    public Inputter(JavaPlugin plugin) {
        instance = this;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        cachedInputters = new HashMap<>();
    }

    private final Map<Player, InputterRunner> cachedInputters;

    public void register(InputterRunner inputterRunner) {
        cachedInputters.put(inputterRunner.getSender(), inputterRunner);
    }

    public void unregister(InputterRunner inputterRunner) {
        cachedInputters.remove(inputterRunner.getSender());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInput(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        if (!cachedInputters.containsKey(sender)) return;
        event.setCancelled(true);
        // run synchronously to avoid concurrency issues
        String messageClone = event.getMessage();
        Scheduler.runTask(() -> {
            cachedInputters.get(sender).runner(messageClone);
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogout(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        cachedInputters.remove(player);
    }

}
