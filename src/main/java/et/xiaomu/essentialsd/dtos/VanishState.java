package et.xiaomu.essentialsd.dtos;

import et.xiaomu.essentialsd.EssentialsD;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VanishState {
    public static Boolean isManualVanished(UUID uuid) {
        String sql = "SELECT uuid FROM vanish_state WHERE uuid = ?;";

        try {
            ResultSet rs = EssentialsD.database.query(sql, uuid.toString());
            try {
                if (rs == null) {
                    return null;
                }
                return rs.next();
            } finally {
                if (rs != null) {
                    rs.close();
                }
            }
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("查询隐身持久化状态失败", e, sql);
            return null;
        }
    }

    public static List<UUID> getAllManualVanished() {
        List<UUID> ids = new ArrayList<>();
        String sql = "SELECT uuid FROM vanish_state;";

        try {
            ResultSet rs = EssentialsD.database.query(sql);
            try {
                if (rs == null) {
                    return ids;
                }
                while (rs.next()) {
                    String uuid = rs.getString("uuid");
                    if (uuid == null || uuid.isBlank()) {
                        continue;
                    }
                    try {
                        ids.add(UUID.fromString(uuid));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                return ids;
            } finally {
                if (rs != null) {
                    rs.close();
                }
            }
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("加载隐身持久化列表失败", e, sql);
            return ids;
        }
    }

    public static boolean setManualVanished(UUID uuid, boolean vanished) {
        if (vanished) {
            return upsert(uuid);
        }
        return delete(uuid);
    }

    private static boolean upsert(UUID uuid) {
        String sql = "INSERT INTO vanish_state (uuid, updated_at) VALUES (?, CURRENT_TIMESTAMP) ON CONFLICT (uuid) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;";

        try {
            ResultSet rs = EssentialsD.database.query(sql, uuid.toString());
            if (rs != null) {
                rs.close();
            }
            return true;
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("保存隐身持久化状态失败", e, sql);
            return false;
        }
    }

    private static boolean delete(UUID uuid) {
        String sql = "DELETE FROM vanish_state WHERE uuid = ?;";

        try {
            ResultSet rs = EssentialsD.database.query(sql, uuid.toString());
            if (rs != null) {
                rs.close();
            }
            return true;
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("删除隐身持久化状态失败", e, sql);
            return false;
        }
    }
}
