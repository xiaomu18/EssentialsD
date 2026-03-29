package cn.lunadeer.essentialsd.managers;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.utils.MuteDuration;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MuteManager {
    private final Map<String, Entry> playerMutes = new ConcurrentHashMap<>();
    private final Map<String, Entry> ipMutes = new ConcurrentHashMap<>();

    public MuteManager() {
        reload();
    }

    public synchronized void reload() {
        playerMutes.clear();
        ipMutes.clear();

        String sql = "SELECT target_type, target_value, target_name, created_by, created_at, expires_at " +
                "FROM mute_record WHERE expires_at IS NULL OR expires_at > ? ORDER BY created_at DESC;";

        try {
            ResultSet rs = EssentialsD.database.query(sql, new Timestamp(System.currentTimeMillis()));
            try {
                if (rs == null) {
                    return;
                }
                while (rs.next()) {
                    Entry entry = new Entry(
                            TargetType.valueOf(rs.getString("target_type")),
                            rs.getString("target_value"),
                            rs.getString("target_name"),
                            rs.getString("created_by"),
                            rs.getTimestamp("created_at").getTime(),
                            rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").getTime()
                    );
                    getTargetMap(entry.targetType).put(entry.targetValue, entry);
                }
            } finally {
                if (rs != null) {
                    rs.close();
                }
            }
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("加载禁言列表失败", e, sql);
        }
    }

    public Entry getMute(Player player) {
        Entry playerMute = getActiveEntry(playerMutes, player.getUniqueId().toString());
        if (playerMute != null) {
            return playerMute;
        }
        String ip = getPlayerIp(player);
        if (ip == null) {
            return null;
        }
        return getActiveEntry(ipMutes, normalizeIp(ip));
    }

    public boolean isSelfDeceptionMode() {
        return "self-deception".equalsIgnoreCase(EssentialsD.config.MUTE_MODE);
    }

    public boolean isBlockedCommand(String message) {
        String root = normalizeCommand(message);
        return root != null && EssentialsD.config.MUTE_BLOCKED_COMMANDS.contains(root);
    }

    public synchronized void mutePlayer(OfflinePlayer player, Long durationMillis, String operatorName) {
        String playerName = player.getName();
        if (playerName == null || playerName.isBlank()) {
            playerName = player.getUniqueId().toString();
        }
        upsertEntry(new Entry(
                TargetType.PLAYER,
                player.getUniqueId().toString(),
                playerName,
                operatorName,
                System.currentTimeMillis(),
                toExpiresAt(durationMillis)
        ));
    }

    public synchronized void muteIp(String ip, String targetName, Long durationMillis, String operatorName) {
        String normalizedIp = normalizeIp(ip);
        String displayName = targetName == null || targetName.isBlank() ? normalizedIp : targetName;
        upsertEntry(new Entry(
                TargetType.IP,
                normalizedIp,
                displayName,
                operatorName,
                System.currentTimeMillis(),
                toExpiresAt(durationMillis)
        ));
    }

    public synchronized boolean unmutePlayer(UUID uuid) {
        playerMutes.remove(uuid.toString());
        return delete(TargetType.PLAYER, uuid.toString());
    }

    public synchronized boolean unmuteIp(String ip) {
        String normalizedIp = normalizeIp(ip);
        ipMutes.remove(normalizedIp);
        return delete(TargetType.IP, normalizedIp);
    }

    public List<Entry> getActiveMutes() {
        List<Entry> entries = new ArrayList<>();
        playerMutes.values().forEach(entry -> addIfActive(entries, entry, playerMutes));
        ipMutes.values().forEach(entry -> addIfActive(entries, entry, ipMutes));
        entries.sort(Comparator.comparing((Entry entry) -> entry.targetType.name()).thenComparing(Entry::getDisplayName));
        return entries;
    }

    public List<String> getMutedPlayerNames() {
        List<String> names = new ArrayList<>();
        for (Entry entry : getActiveMutes()) {
            if (entry.targetType == TargetType.PLAYER) {
                names.add(entry.targetName);
            }
        }
        return names;
    }

    public List<String> getMutedIps() {
        List<String> ips = new ArrayList<>();
        for (Entry entry : getActiveMutes()) {
            if (entry.targetType == TargetType.IP) {
                ips.add(entry.targetValue);
            }
        }
        return ips;
    }

    public static String getPlayerIp(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address == null) {
            return null;
        }
        if (address.getAddress() != null) {
            return address.getAddress().getHostAddress();
        }
        return address.getHostString();
    }

    public static String normalizeIp(String ip) {
        if (ip == null) {
            return null;
        }
        return ip.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeCommand(String message) {
        if (message == null) {
            return null;
        }
        String normalized = message.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            return null;
        }
        int blankIndex = normalized.indexOf(' ');
        String root = blankIndex >= 0 ? normalized.substring(0, blankIndex) : normalized;
        int namespaceIndex = root.indexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex + 1 < root.length()) {
            root = root.substring(namespaceIndex + 1);
        }
        return root.isBlank() ? null : root;
    }

    private Entry getActiveEntry(Map<String, Entry> source, String key) {
        if (key == null) {
            return null;
        }
        Entry entry = source.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            source.remove(key, entry);
            return null;
        }
        return entry;
    }

    private void addIfActive(List<Entry> entries, Entry entry, Map<String, Entry> source) {
        if (entry == null) {
            return;
        }
        if (entry.isExpired()) {
            source.remove(entry.targetValue, entry);
            return;
        }
        entries.add(entry);
    }

    private Map<String, Entry> getTargetMap(TargetType type) {
        return type == TargetType.PLAYER ? playerMutes : ipMutes;
    }

    private Long toExpiresAt(Long durationMillis) {
        return durationMillis == null ? null : System.currentTimeMillis() + durationMillis;
    }

    private void upsertEntry(Entry entry) {
        getTargetMap(entry.targetType).put(entry.targetValue, entry);

        String sql = "INSERT INTO mute_record (target_type, target_value, target_name, created_by, created_at, expires_at) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?) " +
                "ON CONFLICT (target_type, target_value) DO UPDATE SET " +
                "target_name = excluded.target_name, " +
                "created_by = excluded.created_by, " +
                "created_at = excluded.created_at, " +
                "expires_at = excluded.expires_at;";

        try {
            ResultSet rs = EssentialsD.database.query(
                    sql,
                    entry.targetType.name(),
                    entry.targetValue,
                    entry.targetName,
                    entry.createdBy,
                    entry.expiresAt == null ? null : new Timestamp(entry.expiresAt)
            );
            if (rs != null) {
                rs.close();
            }
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("保存禁言记录失败", e, sql);
        }
    }

    private boolean delete(TargetType type, String targetValue) {
        String sql = "DELETE FROM mute_record WHERE target_type = ? AND target_value = ?;";
        try {
            ResultSet rs = EssentialsD.database.query(sql, type.name(), targetValue);
            if (rs != null) {
                rs.close();
            }
            return true;
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("删除禁言记录失败", e, sql);
            return false;
        }
    }

    public enum TargetType {
        PLAYER,
        IP
    }

    public static class Entry {
        public final TargetType targetType;
        public final String targetValue;
        public final String targetName;
        public final String createdBy;
        public final long createdAt;
        public final Long expiresAt;

        public Entry(TargetType targetType, String targetValue, String targetName, String createdBy, long createdAt, Long expiresAt) {
            this.targetType = targetType;
            this.targetValue = targetValue;
            this.targetName = targetName;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }

        public boolean isExpired() {
            return expiresAt != null && expiresAt <= System.currentTimeMillis();
        }

        public String getDisplayName() {
            if (targetType == TargetType.IP) {
                if (targetName == null || targetName.isBlank() || targetName.equals(targetValue)) {
                    return targetValue;
                }
                return targetValue + " (" + targetName + ")";
            }
            return targetName == null || targetName.isBlank() ? targetValue : targetName;
        }

        public String getRemainingText() {
            return MuteDuration.formatRemaining(expiresAt);
        }
    }
}
