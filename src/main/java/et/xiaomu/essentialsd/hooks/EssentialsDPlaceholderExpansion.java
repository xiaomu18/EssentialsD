package et.xiaomu.essentialsd.hooks;

import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.managers.VanishManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EssentialsDPlaceholderExpansion extends PlaceholderExpansion {
    private final EssentialsD plugin;

    public EssentialsDPlaceholderExpansion(EssentialsD plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "essd";
    }

    @Override
    public @NotNull String getAuthor() {
        List<String> authors = plugin.getPluginMeta().getAuthors();
        return authors.isEmpty() ? "unknown" : String.join(", ", authors);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (!"online_without_vanish".equalsIgnoreCase(params)) {
            return null;
        }

        VanishManager vanishManager = EssentialsD.vanishManager;
        if (vanishManager == null) {
            return Integer.toString(Bukkit.getOnlinePlayers().size());
        }

        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int vanishedPlayers = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (vanishManager.isVanished(online)) {
                vanishedPlayers++;
            }
        }
        return Integer.toString(Math.max(0, onlinePlayers - vanishedPlayers));
    }
}
