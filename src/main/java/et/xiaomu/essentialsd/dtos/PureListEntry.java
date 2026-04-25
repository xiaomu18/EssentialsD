package et.xiaomu.essentialsd.dtos;

import et.xiaomu.essentialsd.EssentialsD;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PureListEntry {
    private PureListEntry() {
    }

    public static Map<UUID, Set<UUID>> getAllEntries() {
        Map<UUID, Set<UUID>> entries = new HashMap<>();
        String sql = "SELECT owner_uuid, target_uuid FROM pure_list;";

        try {
            ResultSet rs = EssentialsD.database.query(sql);
            try {
                if (rs == null) {
                    return entries;
                }
                while (rs.next()) {
                    UUID owner = parseUuid(rs.getString("owner_uuid"));
                    UUID target = parseUuid(rs.getString("target_uuid"));
                    if (owner == null || target == null) {
                        continue;
                    }
                    entries.computeIfAbsent(owner, ignored -> new HashSet<>()).add(target);
                }
                return entries;
            } finally {
                if (rs != null) {
                    rs.close();
                }
            }
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("加载纯净列表失败", e, sql);
            return entries;
        }
    }

    public static boolean add(UUID owner, UUID target) {
        String sql = "INSERT INTO pure_list (owner_uuid, target_uuid, created_at) VALUES (?, ?, CURRENT_TIMESTAMP) "
                + "ON CONFLICT (owner_uuid, target_uuid) DO NOTHING;";

        try {
            return EssentialsD.database.execute(sql, owner.toString(), target.toString());
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("添加纯净列表成员失败", e, sql);
            return false;
        }
    }

    public static boolean remove(UUID owner, UUID target) {
        String sql = "DELETE FROM pure_list WHERE owner_uuid = ? AND target_uuid = ?;";

        try {
            return EssentialsD.database.execute(sql, owner.toString(), target.toString());
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("移除纯净列表成员失败", e, sql);
            return false;
        }
    }

    public static boolean clear(UUID owner) {
        String sql = "DELETE FROM pure_list WHERE owner_uuid = ?;";

        try {
            return EssentialsD.database.execute(sql, owner.toString());
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("清空纯净列表失败", e, sql);
            return false;
        }
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
