package et.xiaomu.essentialsd.utils;

import et.xiaomu.essentialsd.dtos.PlayerName;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class PlayerLookup {
    private PlayerLookup() {
    }

    public static @Nullable OfflinePlayer resolve(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            return online;
        }

        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(input));
        } catch (IllegalArgumentException ignored) {
        }

        UUID recorded = PlayerName.getUuid(input);
        if (recorded != null) {
            return Bukkit.getOfflinePlayer(recorded);
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(input);
        if (offline.isOnline() || offline.hasPlayedBefore()) {
            return offline;
        }
        return null;
    }

    public static String displayName(OfflinePlayer player) {
        String name = player.getName();
        return name != null && !name.isBlank() ? name : player.getUniqueId().toString();
    }
}
