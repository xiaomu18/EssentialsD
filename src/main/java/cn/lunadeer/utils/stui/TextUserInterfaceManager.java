package cn.lunadeer.utils.stui;

import cn.lunadeer.utils.Misc;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TextUserInterfaceManager {

    private BukkitAudiences adventure = null;
    private final JavaPlugin plugin;
    private static TextUserInterfaceManager instance;

    public static TextUserInterfaceManager getInstance() {
        return instance;
    }

    public TextUserInterfaceManager(JavaPlugin plugin) {
        instance = this;
        if (!Misc.isPaper()) {
            this.adventure = BukkitAudiences.create(plugin);
        }
        this.plugin = plugin;
    }

    public BukkitAudiences getAdventure() {
        return adventure;
    }

    public void sendMessage(Player player, Component msg) {
        if (adventure != null) {
            this.adventure.player(player).sendMessage(msg);
        } else {
            player.sendMessage(msg);
        }
    }
}
